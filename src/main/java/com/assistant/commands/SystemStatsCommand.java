package com.assistant.commands;

public class SystemStatsCommand implements Command {
    @Override
    public String execute(String input) {
        Runtime runtime = Runtime.getRuntime();
        
        // Информация о памяти
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usedPercentage = (double) usedMemory / totalMemory * 100;
        
        // Информация о процессоре
        int availableProcessors = runtime.availableProcessors();
        
        // Форматируем вывод
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Статистика системы:\n\n");
        stats.append(String.format("💾 Память:\n"));
        stats.append(String.format("  • Использовано: %s / %s (%.1f%%)\n", 
            formatBytes(usedMemory), formatBytes(totalMemory), usedPercentage));
        stats.append(String.format("  • Свободно: %s\n", formatBytes(freeMemory)));
        stats.append(String.format("  • Максимум: %s\n\n", formatBytes(maxMemory)));
        stats.append(String.format("🖥️ Процессор:\n"));
        stats.append(String.format("  • Доступно ядер: %d\n", availableProcessors));
        
        // Предупреждение если память перегружена
        if (usedPercentage >= 90) {
            stats.append("\n⚠️ Внимание: высокая загрузка памяти!");
        }
        
        return stats.toString();
    }
    
    private String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        } else {
            double gb = mb / 1024.0;
            return String.format("%.2f GB", gb);
        }
    }
}
