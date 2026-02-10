package com.aton.proj.oneGasMeteor.handler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.model.tcp.CommandResponse;
import com.aton.proj.oneGasMeteor.model.tcp.TelemetryMessage;
import com.aton.proj.oneGasMeteor.service.tcp.TelemetryProcessor;

@Component
public class TcpConnectionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(TcpConnectionHandler.class);
    private static final int BUFFER_SIZE = 4096;
    
    private final TelemetryProcessor telemetryProcessor;

    public TcpConnectionHandler(TelemetryProcessor telemetryProcessor) {
        this.telemetryProcessor = telemetryProcessor;
    }

    /**
     * Gestisce una singola connessione TCP
     */
    public void handleConnection(Socket socket, int timeout) {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        log.info("New connection from: {}", clientAddress);
        
        try {
            socket.setSoTimeout(timeout);
            
            // 1. Leggi i dati in arrivo
            byte[] receivedData = readData(socket.getInputStream());
            
            if (receivedData.length == 0) {
                log.warn("No data received from {}", clientAddress);
                return;
            }
            
            // 2. Crea il messaggio di telemetria
            TelemetryMessage message = new TelemetryMessage(receivedData, clientAddress);
            
            // 3. Processa e genera risposta
            CommandResponse response = telemetryProcessor.process(message);
            
            // 4. Invia risposta
            sendResponse(socket.getOutputStream(), response);
            
            log.info("Successfully handled connection from {}", clientAddress);
            
        } catch (SocketTimeoutException e) {
            log.error("Timeout reading from {}", clientAddress);
        } catch (IOException e) {
            log.error("Error handling connection from {}: {}", clientAddress, e.getMessage());
        } finally {
            closeSocket(socket, clientAddress);
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

    private void sendResponse(OutputStream outputStream, CommandResponse response) throws IOException {
        outputStream.write(response.binaryData());
        outputStream.flush();
        
        log.debug("Sent response: {} ({} bytes)", 
                  response.asciiCommand(), 
                  response.binaryData().length);
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