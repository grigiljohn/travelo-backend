package com.travelo.mediaservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Service for storing and retrieving files from local disk.
 * Replaces S3 for development and environments without cloud storage.
 */
public interface LocalStorageService {

    /**
     * Save an uploaded file to local storage.
     * @param relativePath Path relative to base storage (e.g. uploads/abc-123/original)
     * @param file Multipart file from request
     * @return Absolute path to saved file
     */
    Path save(String relativePath, MultipartFile file);

    /**
     * Save bytes to local storage.
     */
    Path save(String relativePath, byte[] bytes, String contentType);

    /**
     * Get file by relative path.
     */
    File getFile(String relativePath);

    /**
     * Get input stream for reading file.
     */
    InputStream getInputStream(String relativePath) throws IOException;

    /**
     * Check if file exists.
     */
    boolean exists(String relativePath);

    /**
     * Delete file by relative path.
     */
    boolean delete(String relativePath);

    /**
     * Resolve full path for a relative storage key.
     */
    Path resolvePath(String relativePath);
}
