package com.assistant.commands;

import com.assistant.services.WindowsAutomationService;

import java.util.Locale;

public class NetworkControlCommand implements Command {
    private final WindowsAutomationService windows = new WindowsAutomationService();

    @Override
    public String execute(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);

        if (isWifi(lower)) {
            if (isEnable(lower)) {
                return windows.setWifi(true);
            }
            if (isDisable(lower)) {
                return windows.setWifi(false);
            }
            return windows.openWifiSettings();
        }

        if (isBluetooth(lower)) {
            if (isEnable(lower)) {
                return windows.setBluetooth(true);
            }
            if (isDisable(lower)) {
                return windows.setBluetooth(false);
            }
            return windows.openBluetoothSettings();
        }

        return "Скажите, что сделать с сетью: включи Wi-Fi, выключи Bluetooth, открой настройки Wi-Fi.";
    }

    private boolean isWifi(String lower) {
        return lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("вайфай") || lower.contains("вай-фай");
    }

    private boolean isBluetooth(String lower) {
        return lower.contains("bluetooth") || lower.contains("блютуз") || lower.contains("блютус");
    }

    private boolean isEnable(String lower) {
        return lower.contains("включ") || lower.contains("enable") || lower.contains(" on");
    }

    private boolean isDisable(String lower) {
        return lower.contains("выключ") || lower.contains("отключ") || lower.contains("disable") || lower.contains(" off");
    }
}
