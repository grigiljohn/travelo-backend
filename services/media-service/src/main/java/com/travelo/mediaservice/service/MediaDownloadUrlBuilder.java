package com.travelo.mediaservice.service;

import com.travelo.mediaservice.config.LocalStorageProperties;
import com.travelo.mediaservice.config.MediaPublicUrlProperties;
import com.travelo.mediaservice.config.MediaS3Properties;
import com.travelo.mediaservice.util.MediaS3ObjectKeys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builds {@code downloadUrl} values returned to clients. Blobs may live in S3 while URLs either
 * point at this service's file API or at a public object base (CloudFront / S3 website / public bucket).
 */
@Component
public class MediaDownloadUrlBuilder {

    private final LocalStorageProperties localStorageProperties;
    private final MediaPublicUrlProperties publicUrlProperties;
    private final MediaS3Properties s3Properties;

    public MediaDownloadUrlBuilder(LocalStorageProperties localStorageProperties,
                                   MediaPublicUrlProperties publicUrlProperties,
                                   MediaS3Properties s3Properties) {
        this.localStorageProperties = localStorageProperties;
        this.publicUrlProperties = publicUrlProperties;
        this.s3Properties = s3Properties;
    }

    /** Base URL for {@code GET /v1/media/files/...} (no trailing slash). */
    public String effectiveApiBaseUrl() {
        if (StringUtils.hasText(publicUrlProperties.getApiBaseUrl())) {
            return publicUrlProperties.getApiBaseUrl().replaceAll("/+$", "");
        }
        return localStorageProperties.getBaseUrl().replaceAll("/+$", "");
    }

    public boolean prefersPublicObjectUrls() {
        return s3Properties.isEnabled() && StringUtils.hasText(s3Properties.getPublicObjectBaseUrl());
    }

    /**
     * Primary upload response: API file URL, or direct object URL when {@code media.storage.s3.public-object-base-url} is set.
     */
    public String buildUploadDownloadUrl(UUID mediaId, String relativeStorageKey) {
        if (prefersPublicObjectUrls()) {
            return buildPublicObjectUrl(relativeStorageKey);
        }
        return effectiveApiBaseUrl() + "/v1/media/files/" + mediaId;
    }

    public String buildApiMediaUrl(UUID mediaId, String variantNameOrNull) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(effectiveApiBaseUrl() + "/v1/media/files/" + mediaId);
        if (StringUtils.hasText(variantNameOrNull)) {
            b.queryParam("variant", variantNameOrNull);
        }
        return b.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    /** HTTPS URL to the object in bucket (prefix + relative key), path segments encoded. */
    public String buildPublicObjectUrl(String relativeStorageKey) {
        String base = s3Properties.getPublicObjectBaseUrl().replaceAll("/+$", "");
        String fullKey = MediaS3ObjectKeys.fullObjectKey(s3Properties.getKeyPrefix(), relativeStorageKey);
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base);
        for (String seg : fullKey.split("/")) {
            if (!seg.isEmpty()) {
                b.pathSegment(seg);
            }
        }
        return b.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    /**
     * After existence check: link clients should use. Public mode uses the storage path; API mode uses id + optional variant query.
     */
    public String buildDownloadUrlForContent(UUID mediaId, String relativePath, String variantNameOrNull) {
        if (prefersPublicObjectUrls()) {
            return buildPublicObjectUrl(relativePath);
        }
        return buildApiMediaUrl(mediaId, variantNameOrNull);
    }

    /** Variant row URL when {@code includeSignedUrls} is true. */
    public String buildVariantUrl(UUID mediaId, String variantName, String variantRelativeKey) {
        if (prefersPublicObjectUrls() && StringUtils.hasText(variantRelativeKey)) {
            return buildPublicObjectUrl(variantRelativeKey);
        }
        return buildApiMediaUrl(mediaId, variantName);
    }
}
