package com.jakeberryman.svcdiscordintegration.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakeberryman.svcdiscordintegration.config.Config;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FabricConfig implements Config.ConfigProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("svcdiscordintegration.json");

    private ConfigData config;

    public static void register() {
        FabricConfig fabricConfig = new FabricConfig();
        fabricConfig.load();
        Config.setProvider(fabricConfig);
    }

    private void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
                config = new ConfigData();
            }
        } else {
            config = new ConfigData();
            save();
        }
    }

    private void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getToken() {
        return config.token;
    }

    @Override
    public void setToken(String token) {
        config.token = token;
        save();
    }

    private static class ConfigData {
        public String token = "";
    }
}
