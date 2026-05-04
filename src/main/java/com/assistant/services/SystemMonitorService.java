package com.assistant.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitorService {
    private static final double MEMORY_THRESHOLD = 90.0;

    private final ScheduledExecutorService scheduler;

    public SystemMonitorService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r ->
            Thread.ofVirtual()
                .name("SystemMonitor-Virtual")
                .unstarted(r)
        );
    }

    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkMemoryUsage();
            } catch (Exception e) {
                System.err.println("System monitor error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stopMonitoring() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        double usedPercentage = (double) usedMemory / totalMemory * 100;

        return String.format(
            "Память: %s / %s (%.1f%%)",
            formatBytes(usedMemory),
            formatBytes(totalMemory),
            usedPercentage
        );
    }

    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        double usedPercentage = (double) usedMemory / totalMemory * 100;

        if (usedPercentage >= MEMORY_THRESHOLD) {
            System.err.printf("High memory usage: %.1f%%%n", usedPercentage);
            System.gc();
        }
    }

    private String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        }

        return String.format("%.2f GB", mb / 1024.0);
    }
}
