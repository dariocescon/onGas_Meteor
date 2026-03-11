package com.aton.proj.oneGasMeteor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.config.tcpServer.TcpServerProperties;
import com.aton.proj.oneGasMeteor.handler.TcpConnectionHandlerReadExactly;

import jakarta.annotation.PreDestroy;

@Component
public class TcpSocketServer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(TcpSocketServer.class);

	private final TcpServerProperties properties;
	private final TcpConnectionHandlerReadExactly connectionHandlerReadExactly;
	private final ExecutorService executorService;
	private final Semaphore connectionLimiter;

	private ServerSocket serverSocket;
	private volatile boolean running = false;

	public TcpSocketServer(TcpServerProperties properties,
			TcpConnectionHandlerReadExactly connectionHandlerReadExactly) {
		this.properties = properties;
		this.connectionHandlerReadExactly = connectionHandlerReadExactly;
		this.executorService = Executors.newVirtualThreadPerTaskExecutor(); // Java 21 Virtual Threads
		this.connectionLimiter = new Semaphore(properties.getMaxConnections());
	}

	@Override
	public void run(String... args) {
		startServer();
	}

	private void startServer() {
		try {
			serverSocket = new ServerSocket(properties.getPort(), properties.getBacklog());
			running = true;

			log.info("TCP Server started on port {}", properties.getPort());
			log.info("Timeout configured: {}ms", properties.getTimeout());
			log.info("Max concurrent connections: {}", properties.getMaxConnections());
			log.info("ServerSocket backlog: {}", properties.getBacklog());

			acceptConnections();

		} catch (IOException e) {
			log.error("Failed to start TCP server: {}", e.getMessage(), e);
			throw new RuntimeException("Cannot start TCP server", e);
		}
	}

	private void acceptConnections() {
		while (running) {
			try {
				// Acquisisce un permesso dal semaforo (blocca se raggiunto il limite)
				connectionLimiter.acquire();

				int availablePermits = connectionLimiter.availablePermits();
				if (availablePermits < properties.getMaxConnections() * 0.1) {
					log.warn("Connection limiter running low: {}/{} permits available",
							availablePermits, properties.getMaxConnections());
				}

				Socket clientSocket = serverSocket.accept();

				// Gestisce ogni connessione in un virtual thread separato
				executorService.submit(() -> {
					try {
						connectionHandlerReadExactly.handleConnection(clientSocket, properties.getTimeout());
					} finally {
						connectionLimiter.release();
					}
				});

			} catch (InterruptedException e) {
				if (running) {
					log.warn("Connection limiter interrupted");
					Thread.currentThread().interrupt();
				}
			} catch (IOException e) {
				if (running) {
					log.error("Error accepting connection: {}", e.getMessage());
				}
				// Rilascia il permesso se la connessione non è stata accettata
				connectionLimiter.release();
			}
		}
	}

	@PreDestroy
	public void shutdown() {
		log.info("Shutting down TCP server...");
		running = false;

		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}

			executorService.shutdown();
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}

			log.info("TCP server stopped");
			log.info("Connection limiter final state: {}/{} permits available",
					connectionLimiter.availablePermits(), properties.getMaxConnections());

		} catch (IOException | InterruptedException e) {
			log.error("Error during shutdown: {}", e.getMessage());
		}
	}
}