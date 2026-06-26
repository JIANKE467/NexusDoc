package com.nexusdoc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@MapperScan("com.nexusdoc.mapper")
@SpringBootApplication
public class NexusDocApplication {

    public static void main(String[] args) {
        loadLocalEnv();
        SpringApplication.run(NexusDocApplication.class, args);
    }

    private static void loadLocalEnv() {
        Path envPath = Path.of(".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                loadEnvLine(line);
            }
        } catch (IOException exception) {
            System.err.println("本地 .env 读取失败，请检查文件权限。");
        }
    }

    private static void loadEnvLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }
        int splitIndex = trimmed.indexOf('=');
        if (splitIndex <= 0) {
            return;
        }
        String key = trimmed.substring(0, splitIndex).trim();
        String value = unquote(trimmed.substring(splitIndex + 1).trim());
        if (key.isEmpty() || value.isEmpty()) {
            return;
        }
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
