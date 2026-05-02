package com.assistant.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitorService {
    private final ScheduledExecutorService scheduler;
    private static final double MEMORY_THRESHOLD = 90.0; // Порог в процентах
    
    public SystemMonitorService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SystemMonitor");
            thread.setDaemon(true); // Демон-поток, чтобы не блокировать завершение приложения
            return thread;
        });
    }
    
    /**
     * Запускает мониторинг системных ресурсов
     * Проверка выполняется каждую минуту
     */
    public void startMonitoring() {
        System.out.println("🔍 Мониторинг системы запущен");
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkMemoryUsage();
            } catch (Exception e) {
                System.err.println("Ошибка при мониторинге системы: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES); // Начальная задержка 0, период 1 минута
    }
    
    /**
     * Проверяет использование оперативной памяти
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // Получаем информацию о памяти в байтах
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        // Вычисляем процент использования
        double usedPercentage = (double) usedMemory / totalMemory * 100;
        double usedOfMaxPercentage = (double) usedMemory / maxMemory * 100;
        
        // Форматируем для вывода
        String totalMB = formatBytes(totalMemory);
        String usedMB = formatBytes(usedMemory);
        String freeMB = formatBytes(freeMemory);
        String maxMB = formatBytes(maxMemory);
        
        // Логируем текущее состояние
        System.out.println(String.format(
            "💾 Память: Использовано %s / %s (%.1f%%) | Свободно: %s | Максимум: %s",
            usedMB, totalMB, usedPercentage, freeMB, maxMB
        ));
        
        // Проверяем превышение порога
        if (usedPercentage >= MEMORY_THRESHOLD) {
            System.err.println(String.format(
                "⚠️ ВНИМАНИЕ: Оперативная память перегружена! Использовано %.1f%% (порог: %.0f%%)",
                usedPercentage, MEMORY_THRESHOLD
            ));
            
            // Предлагаем сборку мусора
            System.out.println("🧹 Попытка освободить память...");
            System.gc();
            
            // Проверяем результат после GC
            try {
                Thread.sleep(1000); // Даем время на сборку мусора
                long newUsedMemory = runtime.totalMemory() - runtime.freeMemory();
                double newUsedPercentage = (double) newUsedMemory / runtime.totalMemory() * 100;
                System.out.println(String.format(
                    "✅ После очистки: %.1f%% (освобождено: %s)",
                    newUsedPercentage, formatBytes(usedMemory - newUsedMemory)
                ));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Форматирует байты в читаемый формат (MB)
     */
    private String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        } else {
            double gb = mb / 1024.0;
            return String.format("%.2f GB", gb);
        }
    }
    
    /**
     * Останавливает мониторинг
     */
    public void stopMonitoring() {
        System.out.println("🛑 Остановка мониторинга системы...");
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
    
    /**
     * Получает текущую статистику памяти
     */
    public String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedPercentage = (double) usedMemory / totalMemory * 100;
        
        return String.format(
            "Память: %s / %s (%.1f%%)",
            formatBytes(usedMemory),
            formatBytes(totalMemory),
            usedPercentage
        );
    }
}
