package com.aton.proj.oneGasMeteor.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.model.ProcessingContext;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.repository.ProcessingMetricsRepository;
import com.aton.proj.oneGasMeteor.service.TelemetryService;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

@Component
public class TcpConnectionHandlerReadExactly {

	private static final Logger log = LoggerFactory.getLogger(TcpConnectionHandlerReadExactly.class);
	private static final int HEADER_SIZE = 17;

	@Value("${tcp.server.port:8091}")
	private int tcpPort;

	private final TelemetryService telemetryService;
	private final ProcessingMetricsRepository metricsRepository;

	public TcpConnectionHandlerReadExactly(TelemetryService telemetryService,
			@Nullable ProcessingMetricsRepository metricsRepository) {
		this.telemetryService = telemetryService;
		this.metricsRepository = metricsRepository;
	}

	/**
	 * Gestisce una singola connessione TCP
	 */
	public void handleConnection(Socket socket, int timeout) {
		String clientAddress = socket.getRemoteSocketAddress().toString();
		log.info("New connection from: {}", clientAddress);

		ProcessingContext context = new ProcessingContext(clientAddress);
		TelemetryResponse response = null;

		try {
			socket.setSoTimeout(timeout);

			// 1. Leggi header (17 byte) + body (declaredLength byte) dal socket
			context.startRead();
			byte[] receivedData = readData(socket.getInputStream(), context);
			context.endRead();

			context.setPayloadLengthBytes(receivedData.length);
			log.trace("[TCP PORT {}] Messaggio ricevuto: {} byte (header 17 + body {})",
					socket.getPort(), receivedData.length, receivedData.length - HEADER_SIZE);

			// 2. Crea il messaggio di telemetria
			TelemetryMessage message = new TelemetryMessage(receivedData, clientAddress);
			response = telemetryService.processTelemetry(message, context);

			log.debug("[TCP PORT {}] Telemetry processed successfully for device: {} (type: {})", tcpPort,
					response.getDeviceId(), response.getDeviceType());

			// GENERA RISPOSTA CON COMANDI
			byte[] replyBytes;

			if (response.getCommands() != null && !response.getCommands().isEmpty()) {

				ControllerUtils.enrichResponseWithConcatenatedCommands(response);

				log.debug("Sending {} commands back to device: {}", response.getCommands().size(),
						response.getConcatenatedCommandsAscii());
				log.trace("Commands bytes: {} bytes", response.getConcatenatedCommandsAscii().getBytes().length);
				log.trace("Commands ASCII: {}", response.getConcatenatedCommandsAscii());
				replyBytes = response.getConcatenatedCommandsAscii().getBytes();

			} else {
				// Nessun comando: risposta vuota
				log.debug("No commands for device, sending empty response");
				replyBytes = new byte[0];
			}

			// 4. Invia risposta
			context.startSend();
			sendResponse(socket.getOutputStream(), replyBytes);
			context.endSend();
			context.setResponseSizeBytes(replyBytes.length);

			context.complete(true, null);
			log.debug("Successfully handled connection from {}", clientAddress);

		} catch (SocketTimeoutException e) {
			log.error("Timeout reading from {}", clientAddress);
			context.complete(false, "Timeout: " + e.getMessage());
		} catch (IOException e) {
			log.error("Error handling connection from {}: {}", clientAddress, e.getMessage());
			context.complete(false, "IOException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error handling connection from {}: {}", clientAddress, e.getMessage());
			context.complete(false, e.getClass().getSimpleName() + ": " + e.getMessage());
		} finally {
			closeSocket(socket, clientAddress);

			if (response != null && response.getCommands() != null && !response.getCommands().isEmpty()) {
				telemetryService.markCommandsAsSent(response.getCommands());
				log.debug("Successfully updated {} commands. Marked as SENT", response.getCommands().size());
			}

			// Salva metriche su DB (in try-catch isolato per non impattare il flusso)
			saveMetrics(context);
		}
	}

	/**
	 * Legge un messaggio completo dal socket in due fasi:
	 * 1. Header fisso di 17 byte
	 * 2. Body di lunghezza dichiarata nell'header (campo 10-bit sui byte 15-16)
	 *
	 * Usa readExactly() per garantire la lettura completa anche se il TCP
	 * consegna i dati in più segmenti (comune su reti NB-IoT/CAT-M1).
	 */
	private byte[] readData(InputStream inputStream, ProcessingContext context) throws IOException {

		// Fase 1: leggi esattamente 17 byte di header
		byte[] header = readExactly(inputStream, HEADER_SIZE);

		// Fase 2: estrai la lunghezza del body dall'header
		// Campo 10-bit: bit[7:6] di byte 15 (high 2 bit) + byte 16 (low 8 bit)
		int declaredLength = ((header[15] >> 6) & 0x03) * 256 + (header[16] & 0xFF);
		context.setDeclaredBodyLength(declaredLength);
		log.debug("Header letto — payload dichiarato: {} byte", declaredLength);

		if (declaredLength == 0) {
			return header;
		}

		// Fase 3: leggi esattamente declaredLength byte di body
		byte[] body = readExactly(inputStream, declaredLength);

		// Fase 4: concatena header + body
		byte[] result = new byte[HEADER_SIZE + declaredLength];
		System.arraycopy(header, 0, result, 0, HEADER_SIZE);
		System.arraycopy(body, 0, result, HEADER_SIZE, declaredLength);

		log.debug("Messaggio completo letto: {} byte totali", result.length);
		return result;
	}

	/**
	 * Legge esattamente n byte dall'InputStream, bloccando fino al completamento.
	 * Garantisce la lettura integrale anche se TCP consegna i dati in più segmenti.
	 *
	 * @throws IOException se lo stream viene chiuso prima di leggere n byte
	 */
	private byte[] readExactly(InputStream inputStream, int n) throws IOException {
		byte[] buf = new byte[n];
		int offset = 0;
		while (offset < n) {
			int read = inputStream.read(buf, offset, n - offset);
			if (read == -1) {
				throw new IOException(
						"Stream chiuso dopo " + offset + " di " + n + " byte attesi");
			}
			offset += read;
		}
		return buf;
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

	private void saveMetrics(ProcessingContext context) {
		if (metricsRepository == null) {
			return;
		}
		try {
			metricsRepository.save(context.toEntity());
			log.debug("Processing metrics saved: deviceId={}, totalMs={}",
					context.getDeviceId(), context.getTotalProcessingTimeMs());
		} catch (Exception e) {
			log.warn("Failed to save processing metrics: {}", e.getMessage());
		}
	}
}
