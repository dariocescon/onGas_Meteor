package com.aton.proj.oneGasMeteor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import com.aton.proj.oneGasMeteor.controller.utils.ControllerUtils;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.service.TelemetryService;

/**
 * Configurazione TCP Socket Server per ricevere raw TCP data e inviare comandi
 * ai device (comunicazione bidirezionale)
 * 
 * Supporta sia binary bytes che ASCII hex string
 */
@Configuration
public class TcpServerConfig {

	private static final Logger log = LoggerFactory.getLogger(TcpServerConfig.class);

	@Value("${tcp.server.port:8091}")
	private int tcpPort;

	private final TelemetryService telemetryService;

	public TcpServerConfig(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	/**
	 * Factory per connessioni TCP in ingresso (RAW mode)
	 */
	@Bean
	public AbstractServerConnectionFactory serverConnectionFactory() {
		log.info("🔧 Configuring TCP Socket Server on port {} (RAW mode, bidirectional)", tcpPort);

		TcpNetServerConnectionFactory factory = new TcpNetServerConnectionFactory(tcpPort);

		// ✅ RAW SERIALIZER: Accetta dati raw senza length header
		factory.setSerializer(new ByteArrayRawSerializer());
		factory.setDeserializer(new ByteArrayRawSerializer());

		// ⚠️ IMPORTANTE: Con raw serializer e bidirectional, usa singleUse
		factory.setSingleUse(true); // Chiude connessione dopo ogni messaggio

		// Timeout
		factory.setSoTimeout(10000); // 10 secondi per ricevere/inviare dati

		log.info("✅ TCP Socket Server configured on port {}", tcpPort);

		return factory;
	}

	/**
	 * Canale per messaggi TCP in ingresso
	 */
	@Bean
	public MessageChannel tcpInputChannel() {
		return new DirectChannel();
	}

	/**
	 * Gateway TCP bidirezionale (riceve e invia) Sostituisce
	 * TcpReceivingChannelAdapter per supportare l'invio di comandi
	 */
	@Bean
	public TcpInboundGateway tcpInboundGateway(AbstractServerConnectionFactory serverConnectionFactory) {

		TcpInboundGateway gateway = new TcpInboundGateway();
		gateway.setConnectionFactory(serverConnectionFactory);
		gateway.setRequestChannel(tcpInputChannel());

		log.info("✅ TCP Inbound Gateway (bidirectional) configured");

		return gateway;
	}

	/**
	 * Handler per messaggi TCP in ingresso Processa la telemetria e ritorna i
	 * comandi da inviare al device
	 */
	@Bean
	@ServiceActivator(inputChannel = "tcpInputChannel")
	public GenericHandler<byte[]> tcpMessageHandler() {
		return new GenericHandler<byte[]>() {
			@Override
			public Object handle(byte[] rawBytes, MessageHeaders headers) {

				log.info("🚀 [TCP PORT {}] Received raw TCP data: {} bytes", tcpPort, rawBytes.length);

				try {
					String hexMessage;

					// 🔍 DETECT: Binary hex bytes o ASCII hex string?
					if (isAsciiHexString(rawBytes)) {
						// Ricevuto ASCII string "180A64..." → già in formato hex
						String asciiString = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
						log.info("   📝 Detected ASCII hex string: {} chars", asciiString.length());
						log.debug("   ASCII content (first 50): {}",
								asciiString.substring(0, Math.min(50, asciiString.length())));

						hexMessage = asciiString.trim().replaceAll("\\s+", "");

					} else {
						// Ricevuto binary bytes → converti in hex string
						log.info("   📝 Detected binary bytes");
						log.debug("   Raw bytes (first 50): {}", ControllerUtils
								.bytesToHex(java.util.Arrays.copyOf(rawBytes, Math.min(50, rawBytes.length))));

						hexMessage = ControllerUtils.bytesToHex(rawBytes);
					}

					log.info("   📊 Hex message ready: {} chars", hexMessage.length());

					// Processa telemetria
					TelemetryResponse response = telemetryService.processTelemetry(hexMessage);

					log.info("✅ [TCP PORT {}] Telemetry processed successfully for device: {} (type: {})", tcpPort,
							response.getDeviceId(), response.getDeviceType());

					// ✅ GENERA RISPOSTA CON COMANDI
					byte[] replyBytes;

					if (response.getCommands() != null && !response.getCommands().isEmpty()) {
						// Concatena i comandi in formato: TEK822,cmd1,cmd2,...
						byte[] commandsBytes = ControllerUtils.concatenateCommands(response.getCommands());
						String commandsAscii = new String(commandsBytes, java.nio.charset.StandardCharsets.US_ASCII);

						log.info("   📤 Sending {} commands back to device: {}", response.getCommands().size(),
								commandsAscii);
						log.debug("   📤 Commands bytes: {} bytes", commandsBytes.length);
						log.debug("   📤 Commands HEX: {}", ControllerUtils.bytesToHex(commandsBytes));

						replyBytes = commandsBytes;

					} else {
						// Nessun comando: risposta vuota
						log.info("   ℹ️  No commands for device, sending empty response");
						replyBytes = new byte[0];
					}

					// ✅ RITORNA i bytes di risposta
					return replyBytes;

				} catch (Exception e) {
					log.error("❌ [TCP PORT {}] Error processing TCP message: {}", tcpPort, e.getMessage(), e);

					// In caso di errore, ritorna risposta vuota
					return new byte[0];
				}
			}
		};
	}

	/**
	 * Verifica se i bytes ricevuti sono una stringa ASCII hex (es. "180A64...")
	 * invece di binary bytes (es. [0x18, 0x0A, 0x64])
	 */
	private boolean isAsciiHexString(byte[] bytes) {
		if (bytes.length == 0)
			return false;

		// Conta quanti bytes sono caratteri hex validi
		int validHexCount = 0;

		for (byte b : bytes) {
			char c = (char) b;
			if (Character.isDigit(c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f') || Character.isWhitespace(c)) {
				validHexCount++;
			}
		}

		// Se almeno il 95% dei bytes sono caratteri hex, è ASCII hex string
		double hexPercentage = (double) validHexCount / bytes.length;
		return hexPercentage > 0.95;
	}
}