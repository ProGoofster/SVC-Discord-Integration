package com.jakeberryman.svcdiscordintegration.config;

public class Config {
    private static ConfigProvider provider;

    public static void setProvider(ConfigProvider configProvider) {
        provider = configProvider;
    }

    public static String getToken() {
        return provider != null ? provider.getToken() : "";
    }

    public static void setToken(String token) {
        if (provider != null) {
            provider.setToken(token);
        }
    }

    public interface ConfigProvider {
        String getToken();
        void setToken(String token);
    }
}
