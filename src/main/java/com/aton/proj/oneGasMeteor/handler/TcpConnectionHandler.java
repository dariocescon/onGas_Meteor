package com.aton.proj.oneGasMeteor.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.service.TelemetryService;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

@Component
public class TcpConnectionHandler {

	private static final Logger log = LoggerFactory.getLogger(TcpConnectionHandler.class);
	private static final int BUFFER_SIZE = 4096;

	@Value("${tcp.server.port:8091}")
	private int tcpPort;

	private final TelemetryService telemetryService;

	public TcpConnectionHandler(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	/**
	 * Gestisce una singola connessione TCP
	 */
	public void handleConnection(Socket socket, int timeout) {
		String clientAddress = socket.getRemoteSocketAddress().toString();
		log.info("New connection from: {}", clientAddress);

		TelemetryResponse response = null;

		try {
			socket.setSoTimeout(timeout);

			// 1. Leggi i dati in arrivo
			byte[] receivedData = readData(socket.getInputStream());
			log.info(" [TCP PORT {}] Received raw TCP data: {} bytes", socket.getPort(), receivedData.length);

			if (receivedData.length == 0) {
				log.warn("No data received from {}", clientAddress);
				return;
			}

			// 2. Crea il messaggio di telemetria
			TelemetryMessage message = new TelemetryMessage(receivedData, clientAddress);
			response = telemetryService.processTelemetry(message);

			log.info("  [TCP PORT {}] Telemetry processed successfully for device: {} (type: {})", tcpPort,
					response.getDeviceId(), response.getDeviceType());

			// GENERA RISPOSTA CON COMANDI
			byte[] replyBytes;

			if (response.getCommands() != null && !response.getCommands().isEmpty()) {

				ControllerUtils.enrichResponseWithConcatenatedCommands(response);

				log.info("   Sending {} commands back to device: {}", response.getCommands().size(),
						response.getConcatenatedCommandsAscii());
				log.debug("  Commands bytes: {} bytes", response.getConcatenatedCommandsHex().getBytes().length);
				log.debug("  Commands HEX: {}", response.getConcatenatedCommandsHex());
				log.debug("  Commands ASCII: {}", response.getConcatenatedCommandsAscii());
				replyBytes = response.getConcatenatedCommandsHex().getBytes();

			} else {
				// Nessun comando: risposta vuota
				log.info("    No commands for device, sending empty response");
				replyBytes = new byte[0];
			}

			// 4. Invia risposta
			sendResponse(socket.getOutputStream(), replyBytes);

			log.info("Successfully handled connection from {}", clientAddress);

		} catch (SocketTimeoutException e) {
			log.error("Timeout reading from {}", clientAddress);
		} catch (IOException e) {
			log.error("Error handling connection from {}: {}", clientAddress, e.getMessage());
		} finally {
			closeSocket(socket, clientAddress);

			if (response != null && response.getCommands().size() > 0) {
				telemetryService.markCommandsAsSent(response.getCommands());
				log.info("Successfully updated {} commands. Marked as SENT", response.getCommands().size());
			}
		}
	}

	private byte[] readData(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = inputStream.read(buffer);

		if (bytesRead <= 0) {
			return new byte[0];
		}

		// Copia solo i bytes effettivamente letti
		byte[] result = new byte[bytesRead];
		System.arraycopy(buffer, 0, result, 0, bytesRead);

		log.debug("Read {} bytes from socket", bytesRead);
		return result;
	}

	private void sendResponse(OutputStream outputStream, byte[] response) throws IOException {
		outputStream.write(response);
		outputStream.flush();

		log.debug("Sent response: {} bytes", response.length);
	}

	private void closeSocket(Socket socket, String clientAddress) {
		try {
			socket.close();
			log.debug("Connection closed: {}", clientAddress);
		} catch (IOException e) {
			log.warn("Error closing socket for {}: {}", clientAddress, e.getMessage());
		}
	}
}