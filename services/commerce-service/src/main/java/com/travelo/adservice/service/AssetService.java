package com.travelo.adservice.service;

import com.travelo.adservice.entity.Asset;
import com.travelo.adservice.entity.enums.AssetType;
import com.travelo.adservice.entity.enums.StorageProvider;
import com.travelo.adservice.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(transactionManager = "adTransactionManager")
public class AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * Create an asset record after media is uploaded to media-service (S3)
     *
     * @param mediaFileId      The ID from media-service
     * @param fileUrl          The S3 URL of the uploaded file
     * @param assetType        The type of asset (IMAGE or VIDEO)
     * @param businessAccountId The business account that owns this asset
     * @param uploadedBy       The user who uploaded the asset
     * @return The created Asset entity
     */
    @Transactional(transactionManager = "adTransactionManager")
    public Asset createAsset(Long mediaFileId, String fileUrl, AssetType assetType,
                            UUID businessAccountId, UUID uploadedBy) {
        logger.info("Creating asset record - mediaFileId: {}, url: {}, type: {}, businessAccountId: {}",
                mediaFileId, fileUrl, assetType, businessAccountId);

        Asset asset = new Asset();
        asset.setUrl(fileUrl);
        asset.setType(assetType);
        asset.setStorageProvider(StorageProvider.S3);
        asset.setBusinessAccountId(businessAccountId);
        asset.setUploadedBy(uploadedBy);

        Asset saved = assetRepository.save(asset);
        logger.info("Asset created successfully - id: {}", saved.getId());
        return saved;
    }

    /**
     * Get asset by ID
     *
     * @param id The asset ID
     * @return The Asset entity
     */
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public Asset getAssetById(UUID id) {
        return assetRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));
    }

    /**
     * Get all assets for a business account
     *
     * @param businessAccountId The business account ID
     * @return List of assets
     */
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public java.util.List<Asset> getAssetsByBusinessAccount(UUID businessAccountId) {
        return assetRepository.findByBusinessAccountIdAndDeletedAtIsNull(businessAccountId);
    }
}

