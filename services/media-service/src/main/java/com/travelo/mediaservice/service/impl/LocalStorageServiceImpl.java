package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.config.LocalStorageProperties;
import com.travelo.mediaservice.service.LocalStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(prefix = "media.storage.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalStorageServiceImpl implements LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    private final Path resolvedBasePath;

    public LocalStorageServiceImpl(LocalStorageProperties properties) {
        this.resolvedBasePath = resolveBasePath(properties.getBasePath());
        log.info("Local storage base path: {}", resolvedBasePath);
    }

    /**
     * Resolve base path to an absolute path. Uses user.home when the default
     * relative path would resolve to a temp directory (e.g. Tomcat work dir).
     */
    private static Path resolveBasePath(String basePath) {
        Path path = Paths.get(basePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path resolved = Paths.get(System.getProperty("user.dir")).resolve(path).toAbsolutePath().normalize();
        // If resolved path is under a temp directory, use user.home instead for stability
        String resolvedStr = resolved.toString();
        if (resolvedStr.contains("Temp") || resolvedStr.contains("tomcat") || resolvedStr.contains("work")) {
            Path fallback = Paths.get(System.getProperty("user.home"), "travelo-media", "uploads");
            log.info("Base path resolved to temp directory, using fallback: {}", fallback);
            return fallback;
        }
        return resolved;
    }

    @Override
    public Path save(String relativePath, MultipartFile file) {
        Path fullPath = resolvePath(relativePath);
        try {
            Files.createDirectories(fullPath.getParent());
            log.info(
                    "flow=local_put START relativeKey={} absolutePath={} sizeBytes={} contentType={}",
                    relativePath,
                    fullPath,
                    file.getSize(),
                    file.getContentType());
            file.transferTo(fullPath.toFile());
            log.info("flow=local_put OK relativeKey={} bytesWritten={}", relativePath, file.getSize());
            return fullPath;
        } catch (IOException e) {
            log.error("Failed to save file to {}", relativePath, e);
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    @Override
    public Path save(String relativePath, byte[] bytes, String contentType) {
        Path fullPath = resolvePath(relativePath);
        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, bytes);
            log.debug("Saved {} bytes to {}", bytes.length, fullPath);
            return fullPath;
        } catch (IOException e) {
            log.error("Failed to save file to {}", relativePath, e);
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    @Override
    public File getFile(String relativePath) {
        Path fullPath = resolvePath(relativePath);
        File file = fullPath.toFile();
        return file.exists() ? file : null;
    }

    @Override
    public InputStream getInputStream(String relativePath) throws IOException {
        Path fullPath = resolvePath(relativePath);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + relativePath);
        }
        return new FileInputStream(fullPath.toFile());
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolvePath(relativePath));
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            return Files.deleteIfExists(resolvePath(relativePath));
        } catch (IOException e) {
            log.warn("Failed to delete {}", relativePath, e);
            return false;
        }
    }

    @Override
    public Path resolvePath(String relativePath) {
        return resolvedBasePath.resolve(relativePath).normalize();
    }
}
