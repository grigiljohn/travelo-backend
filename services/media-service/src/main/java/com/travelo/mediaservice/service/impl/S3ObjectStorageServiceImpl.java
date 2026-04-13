package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.config.MediaS3Properties;
import com.travelo.mediaservice.service.LocalStorageService;
import com.travelo.mediaservice.util.MediaS3ObjectKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores media blobs in S3. Implements {@link LocalStorageService} so upload, download, FFmpeg, and thumbnails
 * keep working; {@link #getFile} and {@link #resolvePath} materialize to a temp cache on disk when needed.
 */
@Service
@ConditionalOnProperty(prefix = "media.storage.s3", name = "enabled", havingValue = "true")
public class S3ObjectStorageServiceImpl implements LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageServiceImpl.class);

    private final S3Client s3Client;
    private final MediaS3Properties s3Properties;
    private final Path cacheDir;

    public S3ObjectStorageServiceImpl(
            @Qualifier("mediaServiceS3Client") S3Client s3Client,
            MediaS3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "travelo-media-s3-cache").toAbsolutePath();
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create S3 local cache dir: " + cacheDir, e);
        }
    }

    private String objectKey(String relativePath) {
        return MediaS3ObjectKeys.fullObjectKey(s3Properties.getKeyPrefix(), relativePath);
    }

    @Override
    public Path save(String relativePath, MultipartFile file) {
        String key = objectKey(relativePath);
        long size = file.getSize();
        String bucket = s3Properties.getBucket();
        log.info(
                "flow=s3_put START bucket={} region={} key={} relativePath={} sizeBytes={} contentType={} originalFilename={}",
                bucket,
                s3Properties.getRegion(),
                key,
                relativePath,
                size,
                file.getContentType(),
                file.getOriginalFilename());
        try {
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength(size);
            if (file.getContentType() != null && !file.getContentType().isBlank()) {
                b.contentType(file.getContentType());
            }
            try (InputStream in = file.getInputStream()) {
                s3Client.putObject(b.build(), RequestBody.fromInputStream(in, size));
            }
            log.info("flow=s3_put OK bucket={} key={} sizeBytes={}", bucket, key, size);
            return cachePathForKey(key);
        } catch (S3Exception e) {
            log.error(
                    "flow=s3_put FAIL S3Exception bucket={} key={} statusCode={} requestId={} extendedRequestId={} errorCode={} errorMessage={}",
                    bucket,
                    key,
                    e.statusCode(),
                    sdkRequestId(e),
                    sdkExtendedRequestId(e),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(),
                    e);
            String awsMsg = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new RuntimeException("S3 PutObject failed: " + key + " — " + awsMsg, e);
        } catch (IOException e) {
            log.error("flow=s3_put FAIL IOException bucket={} key={}", bucket, key, e);
            throw new RuntimeException("S3 upload IO failed: " + key, e);
        }
    }

    @Override
    public Path save(String relativePath, byte[] bytes, String contentType) {
        String key = objectKey(relativePath);
        String bucket = s3Properties.getBucket();
        log.info(
                "flow=s3_put_bytes START bucket={} region={} key={} sizeBytes={} contentType={}",
                bucket,
                s3Properties.getRegion(),
                key,
                bytes.length,
                contentType);
        try {
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength((long) bytes.length);
            if (contentType != null && !contentType.isBlank()) {
                b.contentType(contentType);
            }
            s3Client.putObject(b.build(), RequestBody.fromBytes(bytes));
            log.info("flow=s3_put_bytes OK bucket={} key={}", bucket, key);
            return cachePathForKey(key);
        } catch (S3Exception e) {
            log.error(
                    "flow=s3_put_bytes FAIL S3Exception bucket={} key={} statusCode={} requestId={} errorMessage={}",
                    bucket,
                    key,
                    e.statusCode(),
                    sdkRequestId(e),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(),
                    e);
            throw new RuntimeException("S3 PutObject (bytes) failed: " + key, e);
        }
    }

    @Override
    public File getFile(String relativePath) {
        if (!exists(relativePath)) {
            return null;
        }
        String key = objectKey(relativePath);
        Path target = cachePathForKey(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = s3Client.getObject(
                    GetObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build())) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toFile();
        } catch (IOException e) {
            log.error("S3 materialize to file failed key={}", key, e);
            return null;
        }
    }

    @Override
    public InputStream getInputStream(String relativePath) throws IOException {
        String key = objectKey(relativePath);
        return s3Client.getObject(GetObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build());
    }

    @Override
    public boolean exists(String relativePath) {
        String key = objectKey(relativePath);
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public boolean delete(String relativePath) {
        String key = objectKey(relativePath);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build());
            Files.deleteIfExists(cachePathForKey(key));
            return true;
        } catch (Exception e) {
            log.warn("S3 delete failed key={}", key, e);
            return false;
        }
    }

    @Override
    public Path resolvePath(String relativePath) {
        File f = getFile(relativePath);
        if (f == null) {
            throw new IllegalStateException("S3 object not found for key: " + objectKey(relativePath));
        }
        return f.toPath();
    }

    private Path cachePathForKey(String s3Key) {
        int h = s3Key.hashCode();
        String safe = Integer.toHexString(h) + "_" + s3Key.replace('/', '_');
        return cacheDir.resolve(safe).normalize();
    }

    private static String sdkRequestId(S3Exception e) {
        String id = e.requestId();
        return (id != null && !id.isEmpty()) ? id : null;
    }

    private static String sdkExtendedRequestId(S3Exception e) {
        String id = e.extendedRequestId();
        return (id != null && !id.isEmpty()) ? id : null;
    }
}
