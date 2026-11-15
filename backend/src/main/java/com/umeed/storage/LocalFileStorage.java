package com.umeed.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Keeps files in a folder on the local disk. Intended for development;
 * a cloud implementation can replace it without changing any caller.
 */
@Component
class LocalFileStorage implements FileStorage {

    private static final Pattern VALID_KEY =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*(/[A-Za-z0-9][A-Za-z0-9_.-]*)*");

    private final Path root;

    LocalFileStorage(@Value("${umeed.storage.local.root:${user.home}/umeed-storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target);
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    @Override
    public String urlFor(String key) {
        return "/api/v1/files/" + key;
    }

    private Path resolve(String key) {
        if (key == null || key.length() > 300 || key.contains("..") || !VALID_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
