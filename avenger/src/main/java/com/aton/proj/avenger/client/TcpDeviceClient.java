package com.aton.proj.avenger.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aton.proj.avenger.model.SimulatedDevice;

/**
 * Handles a single TCP connection to the oneGasMeteor server:
 * connects, sends the binary TEK payload, reads the response, then closes.
 */
public class TcpDeviceClient {

    private static final Logger log = LoggerFactory.getLogger(TcpDeviceClient.class);

    private final String host;
    private final int port;
    private final int timeoutMs;

    public TcpDeviceClient(String host, int port, int timeoutSeconds) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutSeconds * 1000;
    }

    /**
     * Sends the given payload on behalf of the device and returns a {@link ConnectionResult}.
     *
     * @param device  the simulated device (used only for logging)
     * @param payload the binary TEK message to send
     * @return result describing bytes sent, bytes received, and any error
     */
    public ConnectionResult send(SimulatedDevice device, byte[] payload) {
        long start = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            OutputStream out = socket.getOutputStream();
            out.write(payload);
            out.flush();
            int bytesSent = payload.length;

            // Read server response (non-blocking read up to timeout)
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesReceived = 0;
            try {
                bytesReceived = in.read(buffer, 0, buffer.length);
                if (bytesReceived < 0) {
                    bytesReceived = 0;
                }
            } catch (IOException readEx) {
                // Timeout or EOF on response read — acceptable
                log.debug("IMEI={} response read ended: {}", device.getImei(), readEx.getMessage());
            }

            long elapsed = System.currentTimeMillis() - start;
            if (bytesReceived > 0) {
                String responseHex = bytesToHex(buffer, bytesReceived);
                log.info("IMEI={} [{}] SUCCESS sent={} B recv={} B elapsed={}ms response={}",
                        device.getImei(), device.getDeviceTypeName(),
                        bytesSent, bytesReceived, elapsed, responseHex);
            } else {
                log.info("IMEI={} [{}] SUCCESS sent={} B no-response elapsed={}ms",
                        device.getImei(), device.getDeviceTypeName(), bytesSent, elapsed);
            }

            return ConnectionResult.success(bytesSent, bytesReceived, elapsed);

        } catch (IOException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("IMEI={} [{}] ERROR elapsed={}ms reason={}",
                    device.getImei(), device.getDeviceTypeName(), elapsed, ex.getMessage());
            return ConnectionResult.failure(elapsed, ex.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    /**
     * Immutable result of a single device TCP connection attempt.
     */
    public static final class ConnectionResult {

        private final boolean success;
        private final int bytesSent;
        private final int bytesReceived;
        private final long elapsedMs;
        private final String errorMessage;

        private ConnectionResult(boolean success, int bytesSent, int bytesReceived,
                                  long elapsedMs, String errorMessage) {
            this.success = success;
            this.bytesSent = bytesSent;
            this.bytesReceived = bytesReceived;
            this.elapsedMs = elapsedMs;
            this.errorMessage = errorMessage;
        }

        public static ConnectionResult success(int bytesSent, int bytesReceived, long elapsedMs) {
            return new ConnectionResult(true, bytesSent, bytesReceived, elapsedMs, null);
        }

        public static ConnectionResult failure(long elapsedMs, String errorMessage) {
            return new ConnectionResult(false, 0, 0, elapsedMs, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getBytesSent() {
            return bytesSent;
        }

        public int getBytesReceived() {
            return bytesReceived;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
