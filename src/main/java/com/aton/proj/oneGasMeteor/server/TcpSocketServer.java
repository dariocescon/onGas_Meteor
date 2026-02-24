package com.aton.proj.oneGasMeteor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.config.tcp.TcpServerProperties;
import com.aton.proj.oneGasMeteor.handler.TcpConnectionHandler;

import jakarta.annotation.PreDestroy;

@Component
public class TcpSocketServer implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(TcpSocketServer.class);
    
    private final TcpServerProperties properties;
    private final TcpConnectionHandler connectionHandler;
    private final ExecutorService executorService;
    
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public TcpSocketServer(TcpServerProperties properties, 
                          TcpConnectionHandler connectionHandler) {
        this.properties = properties;
        this.connectionHandler = connectionHandler;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor(); // Java 21 Virtual Threads
    }

    @Override
    public void run(String... args) {
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(properties.getPort());
            running = true;
            
            log.info("TCP Server started on port {}", properties.getPort());
            log.info("Timeout configured: {}ms", properties.getTimeout());
            
            acceptConnections();
            
        } catch (IOException e) {
            log.error("Failed to start TCP server: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start TCP server", e);
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Gestisce ogni connessione in un virtual thread separato
                executorService.submit(() -> 
                    connectionHandler.handleConnection(clientSocket, properties.getTimeout())
                );
                
            } catch (IOException e) {
                if (running) {
                    log.error("Error accepting connection: {}", e.getMessage());
                }
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
            
        } catch (IOException | InterruptedException e) {
            log.error("Error during shutdown: {}", e.getMessage());
        }
    }
}