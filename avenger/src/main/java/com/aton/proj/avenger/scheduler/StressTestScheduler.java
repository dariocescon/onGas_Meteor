package com.aton.proj.avenger.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aton.proj.avenger.client.TcpDeviceClient;
import com.aton.proj.avenger.client.TcpDeviceClient.ConnectionResult;
import com.aton.proj.avenger.config.AvengerProperties;
import com.aton.proj.avenger.model.SimulatedDevice;
import com.aton.proj.avenger.protocol.TekProtocolBuilder;
import com.aton.proj.avenger.registry.DeviceRegistry;

/**
 * Scheduled stress-test runner.
 *
 * <p>Every minute:
 * <ol>
 *   <li>Selects 200-400 random devices from the registry</li>
 *   <li>For each device, builds a random TEK message (type 4, 8, or 9)</li>
 *   <li>Sends the payload via an independent TCP connection using virtual threads</li>
 *   <li>Logs individual and batch summary results</li>
 * </ol>
 */
@Component
public class StressTestScheduler {

    private static final Logger log = LoggerFactory.getLogger(StressTestScheduler.class);

    /** Valid TEK message types supported by oneGasMeteor */
    private static final int[] MSG_TYPES = {4, 8, 9};

    /** Variation around perBatch for random batch sizing (±25%) */
    private static final double BATCH_VARIATION = 0.25;

    private final DeviceRegistry deviceRegistry;
    private final AvengerProperties properties;
    private final TekProtocolBuilder protocolBuilder;
    private final Random random;

    public StressTestScheduler(DeviceRegistry deviceRegistry, AvengerProperties properties) {
        this.deviceRegistry = deviceRegistry;
        this.properties = properties;
        this.random = new Random();
        this.protocolBuilder = new TekProtocolBuilder(random);
    }

    @Scheduled(fixedRateString = "${avenger.batch.interval-ms:60000}")
    public void runBatch() {
        List<SimulatedDevice> allDevices = new ArrayList<>(deviceRegistry.getAllDevices());
        Collections.shuffle(allDevices, random);

        int perBatch = properties.getDevices().getPerBatch();
        int variation = Math.max(1, (int) (perBatch * BATCH_VARIATION));
        int batchSize = perBatch - variation + random.nextInt(2 * variation + 1);
        batchSize = Math.max(1, Math.min(batchSize, allDevices.size()));
        List<SimulatedDevice> batch = allDevices.subList(0, batchSize);

        log.info("=== Batch START: {} devices selected from registry of {} ===",
                batchSize, allDevices.size());
        long batchStart = System.currentTimeMillis();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalElapsedMs = new AtomicLong(0);

        TcpDeviceClient client = new TcpDeviceClient(
                properties.getTarget().getHost(),
                properties.getTarget().getPort(),
                properties.getTcp().getTimeoutSeconds());

        List<Future<?>> futures = new ArrayList<>(batchSize);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SimulatedDevice device : batch) {
                futures.add(executor.submit(() -> {
                    int msgType = MSG_TYPES[random.nextInt(MSG_TYPES.length)];
                    int measurementCount = properties.getMeasurements().getMin()
                            + random.nextInt(properties.getMeasurements().getMax()
                                            - properties.getMeasurements().getMin() + 1);

                    byte[] payload = protocolBuilder.build(device, msgType, measurementCount);
                    ConnectionResult result = client.send(device, payload);

                    totalElapsedMs.addAndGet(result.getElapsedMs());
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                }));
            }

            // Wait for all tasks to complete (with generous timeout)
            executor.shutdown();
            boolean finished = executor.awaitTermination(
                    properties.getTcp().getTimeoutSeconds() + 30L, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("Batch did not complete within the allotted timeout; some tasks may still be running.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Batch interrupted: {}", ex.getMessage());
        }

        long batchElapsed = System.currentTimeMillis() - batchStart;
        int total = successCount.get() + failureCount.get();
        long avgElapsedMs = total > 0 ? totalElapsedMs.get() / total : 0;

        log.info("=== Batch END: total={} success={} failed={} avgResponseMs={} batchDurationMs={} ===",
                total, successCount.get(), failureCount.get(), avgElapsedMs, batchElapsed);
    }
}
