package com.umeed.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

/**
 * Serves stored photos. Like everything except the public endpoints, it needs a valid token.
 */
@RestController
class FileController {

    private static final String PREFIX = "/api/v1/files/";

    private static final Map<String, MediaType> TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.parseMediaType("image/webp"));

    private final FileStorage fileStorage;

    FileController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping(PREFIX + "**")
    ResponseEntity<Resource> download(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int start = uri.indexOf(PREFIX);
        String key = start >= 0 ? uri.substring(start + PREFIX.length()) : "";
        int dot = key.lastIndexOf('.');
        MediaType type = dot >= 0 ? TYPES.get(key.substring(dot + 1)) : null;
        if (type == null) {
            throw notFound();
        }
        InputStream content;
        try {
            content = fileStorage.open(key);
        } catch (IllegalArgumentException | IOException e) {
            throw notFound();
        }
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(new InputStreamResource(content));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
    }
}
