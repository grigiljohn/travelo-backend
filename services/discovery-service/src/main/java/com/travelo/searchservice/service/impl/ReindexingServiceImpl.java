package com.travelo.searchservice.service.impl;

import com.travelo.searchservice.client.PostServiceClient;
import com.travelo.searchservice.client.ShopServiceClient;
import com.travelo.searchservice.document.*;
import com.travelo.searchservice.service.ReindexingService;
import com.travelo.searchservice.service.SearchIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of re-indexing service.
 * Fetches data from source services and indexes them in Elasticsearch.
 */
@Service
public class ReindexingServiceImpl implements ReindexingService {

    private static final Logger logger = LoggerFactory.getLogger(ReindexingServiceImpl.class);
    private static final int BATCH_SIZE = 50; // Process 50 items at a time
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");

    private final PostServiceClient postServiceClient;
    private final ShopServiceClient shopServiceClient;
    private final SearchIndexingService indexingService;
    private final JdbcTemplate authJdbcTemplate;

    public ReindexingServiceImpl(
            PostServiceClient postServiceClient,
            ShopServiceClient shopServiceClient,
            SearchIndexingService indexingService,
            @Qualifier("authJdbcTemplate") JdbcTemplate authJdbcTemplate) {
        this.postServiceClient = postServiceClient;
        this.shopServiceClient = shopServiceClient;
        this.indexingService = indexingService;
        this.authJdbcTemplate = authJdbcTemplate;
    }

    @Override
    public int reindexPosts() {
        logger.info("Starting re-indexing of posts...");
        int totalIndexed = 0;
        int page = 1;
        int errors = 0;

        try {
            while (true) {
                List<PostServiceClient.PostDto> posts = postServiceClient.getAllPosts(page, BATCH_SIZE);
                
                if (posts == null || posts.isEmpty()) {
                    break; // No more posts
                }

                logger.info("Processing page {} with {} posts", page, posts.size());

                for (PostServiceClient.PostDto postDto : posts) {
                    try {
                        PostDocument postDoc = convertToPostDocument(postDto);
                        indexingService.indexPost(postDoc);
                        totalIndexed++;
                    } catch (Exception e) {
                        errors++;
                        logger.error("Error indexing post {}: {}", postDto.getId(), e.getMessage());
                    }
                }

                // If we got fewer posts than requested, we've reached the end
                if (posts.size() < BATCH_SIZE) {
                    break;
                }

                page++;
            }

            logger.info("Re-indexing of posts completed. Indexed: {}, Errors: {}", totalIndexed, errors);
        } catch (Exception e) {
            logger.error("Error during post re-indexing: {}", e.getMessage(), e);
        }

        return totalIndexed;
    }

    @Override
    public int reindexReels() {
        logger.info("Starting re-indexing of reels...");
        int totalIndexed = 0;
        int page = 1;
        int errors = 0;
        int totalPostsFetched = 0;

        try {
            while (true) {
                List<PostServiceClient.PostDto> posts = postServiceClient.getAllPosts(page, BATCH_SIZE);
                
                if (posts == null || posts.isEmpty()) {
                    logger.info("No more posts found at page {}, stopping", page);
                    break;
                }

                totalPostsFetched += posts.size();
                logger.info("Fetched {} posts from page {} (total so far: {})", posts.size(), page, totalPostsFetched);

                // Debug: Log post types found
                Map<String, Long> postTypeCounts = posts.stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getPostType() != null ? p.getPostType() : "null",
                                Collectors.counting()
                        ));
                logger.info("Post types in page {}: {}", page, postTypeCounts);

                // Filter for reels only
                List<PostServiceClient.PostDto> reels = posts.stream()
                        .filter(p -> {
                            String postType = p.getPostType();
                            boolean isReel = "REEL".equalsIgnoreCase(postType);
                            if (!isReel && postType != null) {
                                logger.debug("Post {} has postType='{}', not a reel", p.getId(), postType);
                            }
                            return isReel;
                        })
                        .collect(Collectors.toList());

                logger.info("Processing page {} with {} reels (filtered from {} posts)", 
                        page, reels.size(), posts.size());

                for (PostServiceClient.PostDto reelDto : reels) {
                    try {
                        PostDocument postDoc = convertToPostDocument(reelDto);
                        indexingService.indexPost(postDoc);
                        totalIndexed++;
                    } catch (Exception e) {
                        errors++;
                        logger.error("Error indexing reel {}: {}", reelDto.getId(), e.getMessage());
                    }
                }

                if (posts.size() < BATCH_SIZE) {
                    break;
                }

                page++;
            }

            logger.info("Re-indexing of reels completed. Indexed: {}, Errors: {}", totalIndexed, errors);
        } catch (Exception e) {
            logger.error("Error during reel re-indexing: {}", e.getMessage(), e);
        }

        return totalIndexed;
    }

    @Override
    public int reindexShops() {
        logger.info("Starting re-indexing of shops...");
        int totalIndexed = 0;
        int page = 1;
        int errors = 0;

        try {
            while (true) {
                List<ShopServiceClient.ShopDto> shops = shopServiceClient.getAllShops(page, BATCH_SIZE);
                
                if (shops == null || shops.isEmpty()) {
                    break;
                }

                logger.info("Processing page {} with {} shops", page, shops.size());

                for (ShopServiceClient.ShopDto shopDto : shops) {
                    try {
                        ShopDocument shopDoc = convertToShopDocument(shopDto);
                        indexingService.indexShop(shopDoc);
                        totalIndexed++;
                    } catch (Exception e) {
                        errors++;
                        logger.error("Error indexing shop {}: {}", shopDto.getId(), e.getMessage());
                    }
                }

                if (shops.size() < BATCH_SIZE) {
                    break;
                }

                page++;
            }

            logger.info("Re-indexing of shops completed. Indexed: {}, Errors: {}", totalIndexed, errors);
        } catch (Exception e) {
            logger.error("Error during shop re-indexing: {}", e.getMessage(), e);
        }

        return totalIndexed;
    }

    @Override
    public int reindexProducts() {
        logger.info("Starting re-indexing of products...");
        int totalIndexed = 0;
        int errors = 0;

        try {
            // First, get all shops
            int shopPage = 1;
            while (true) {
                List<ShopServiceClient.ShopDto> shops = shopServiceClient.getAllShops(shopPage, BATCH_SIZE);
                
                if (shops == null || shops.isEmpty()) {
                    break;
                }

                // For each shop, get all products
                for (ShopServiceClient.ShopDto shop : shops) {
                    int productPage = 1;
                    while (true) {
                        List<ShopServiceClient.ProductDto> products = 
                                shopServiceClient.getShopProducts(shop.getId(), productPage, BATCH_SIZE);
                        
                        if (products == null || products.isEmpty()) {
                            break;
                        }

                        logger.info("Processing shop {} - page {} with {} products", 
                                shop.getId(), productPage, products.size());

                        for (ShopServiceClient.ProductDto productDto : products) {
                            try {
                                ProductDocument productDoc = convertToProductDocument(productDto);
                                indexingService.indexProduct(productDoc);
                                totalIndexed++;
                            } catch (Exception e) {
                                errors++;
                                logger.error("Error indexing product {}: {}", productDto.getId(), e.getMessage());
                            }
                        }

                        if (products.size() < BATCH_SIZE) {
                            break;
                        }

                        productPage++;
                    }
                }

                if (shops.size() < BATCH_SIZE) {
                    break;
                }

                shopPage++;
            }

            logger.info("Re-indexing of products completed. Indexed: {}, Errors: {}", totalIndexed, errors);
        } catch (Exception e) {
            logger.error("Error during product re-indexing: {}", e.getMessage(), e);
        }

        return totalIndexed;
    }

    @Override
    public ReindexingResult reindexAll() {
        logger.info("Starting full re-indexing of all data...");
        
        int postsIndexed = reindexPosts();
        int shopsIndexed = reindexShops();
        int productsIndexed = reindexProducts();
        
        String message = String.format(
                "Re-indexing completed. Posts: %d, Shops: %d, Products: %d",
                postsIndexed, shopsIndexed, productsIndexed
        );
        
        logger.info(message);
        
        return new ReindexingResult(postsIndexed, shopsIndexed, productsIndexed, 0, message);
    }

    /**
     * Convert PostDto to PostDocument.
     */
    private PostDocument convertToPostDocument(PostServiceClient.PostDto postDto) {
        PostDocument doc = new PostDocument();
        doc.setId(postDto.getId());
        doc.setUserId(postDto.getAuthorId());
        doc.setUsername(postDto.getUsername());
        doc.setCaption(postDto.getCaption());
        doc.setPostType(postDto.getPostType());
        doc.setMood(postDto.getMood());
        doc.setLocation(postDto.getLocation());
        
        // Extract thumbnail URL
        String thumbnailUrl = postDto.getThumbnailUrl();
        if (thumbnailUrl == null && postDto.getMediaItems() != null && !postDto.getMediaItems().isEmpty()) {
            PostServiceClient.PostDto.MediaItem firstMedia = postDto.getMediaItems().get(0);
            thumbnailUrl = firstMedia.getThumbnailUrl();
            if (thumbnailUrl == null) {
                thumbnailUrl = firstMedia.getDownloadUrl();
            }
        }
        doc.setThumbnailUrl(thumbnailUrl);
        
        // Extract media URLs
        if (postDto.getMediaItems() != null) {
            List<String> mediaUrls = postDto.getMediaItems().stream()
                    .map(PostServiceClient.PostDto.MediaItem::getDownloadUrl)
                    .filter(url -> url != null)
                    .collect(Collectors.toList());
            doc.setMediaUrls(mediaUrls);
        }
        
        // Extract hashtags from caption
        if (postDto.getCaption() != null) {
            List<String> tags = HASHTAG_PATTERN.matcher(postDto.getCaption())
                    .results()
                    .map(m -> m.group())
                    .distinct()
                    .collect(Collectors.toList());
            doc.setTags(tags);
        }
        
        doc.setLikes(postDto.getLikes() != null ? postDto.getLikes() : 0);
        doc.setComments(postDto.getComments() != null ? postDto.getComments() : 0);
        doc.setShares(postDto.getShares() != null ? postDto.getShares() : 0);
        
        // Parse dates
        try {
            if (postDto.getCreatedAt() != null) {
                doc.setCreatedAt(OffsetDateTime.parse(postDto.getCreatedAt()));
            } else {
                doc.setCreatedAt(OffsetDateTime.now());
            }
            if (postDto.getUpdatedAt() != null) {
                doc.setUpdatedAt(OffsetDateTime.parse(postDto.getUpdatedAt()));
            } else {
                doc.setUpdatedAt(OffsetDateTime.now());
            }
        } catch (Exception e) {
            logger.warn("Error parsing dates for post {}: {}", postDto.getId(), e.getMessage());
            doc.setCreatedAt(OffsetDateTime.now());
            doc.setUpdatedAt(OffsetDateTime.now());
        }
        
        return doc;
    }

    /**
     * Convert ShopDto to ShopDocument.
     */
    private ShopDocument convertToShopDocument(ShopServiceClient.ShopDto shopDto) {
        ShopDocument doc = new ShopDocument();
        doc.setId(shopDto.getId());
        doc.setBusinessAccountId(shopDto.getBusinessAccountId());
        doc.setName(shopDto.getName());
        doc.setDescription(shopDto.getDescription());
        doc.setCategory(shopDto.getCategory());
        doc.setProfileImageUrl(shopDto.getProfileImageUrl());
        doc.setCoverImageUrl(shopDto.getCoverImageUrl());
        doc.setIsActive(shopDto.getIsActive() != null ? shopDto.getIsActive() : true);
        doc.setIsVerified(shopDto.getIsVerified() != null ? shopDto.getIsVerified() : false);
        doc.setProductCount(shopDto.getProductCount() != null ? shopDto.getProductCount() : 0L);
        doc.setFollowerCount(shopDto.getFollowerCount() != null ? shopDto.getFollowerCount() : 0L);
        
        try {
            if (shopDto.getCreatedAt() != null) {
                doc.setCreatedAt(OffsetDateTime.parse(shopDto.getCreatedAt()));
            } else {
                doc.setCreatedAt(OffsetDateTime.now());
            }
            if (shopDto.getUpdatedAt() != null) {
                doc.setUpdatedAt(OffsetDateTime.parse(shopDto.getUpdatedAt()));
            } else {
                doc.setUpdatedAt(OffsetDateTime.now());
            }
        } catch (Exception e) {
            logger.warn("Error parsing dates for shop {}: {}", shopDto.getId(), e.getMessage());
            doc.setCreatedAt(OffsetDateTime.now());
            doc.setUpdatedAt(OffsetDateTime.now());
        }
        
        return doc;
    }

    /**
     * Convert ProductDto to ProductDocument.
     */
    private ProductDocument convertToProductDocument(ShopServiceClient.ProductDto productDto) {
        ProductDocument doc = new ProductDocument();
        doc.setId(productDto.getId());
        doc.setShopId(productDto.getShopId());
        doc.setName(productDto.getName());
        doc.setDescription(productDto.getDescription());
        doc.setCategory(productDto.getCategory());
        doc.setPrice(productDto.getPrice());
        doc.setCurrency(productDto.getCurrency() != null ? productDto.getCurrency() : "USD");
        doc.setThumbnailUrl(productDto.getThumbnailUrl());
        doc.setIsAvailable(productDto.getIsAvailable() != null ? productDto.getIsAvailable() : true);
        doc.setIsFeatured(productDto.getIsFeatured() != null ? productDto.getIsFeatured() : false);
        doc.setViewCount(productDto.getViewCount() != null ? productDto.getViewCount() : 0L);
        doc.setLikeCount(productDto.getLikeCount() != null ? productDto.getLikeCount() : 0L);
        
        try {
            if (productDto.getCreatedAt() != null) {
                doc.setCreatedAt(OffsetDateTime.parse(productDto.getCreatedAt()));
            } else {
                doc.setCreatedAt(OffsetDateTime.now());
            }
            if (productDto.getUpdatedAt() != null) {
                doc.setUpdatedAt(OffsetDateTime.parse(productDto.getUpdatedAt()));
            } else {
                doc.setUpdatedAt(OffsetDateTime.now());
            }
        } catch (Exception e) {
            logger.warn("Error parsing dates for product {}: {}", productDto.getId(), e.getMessage());
            doc.setCreatedAt(OffsetDateTime.now());
            doc.setUpdatedAt(OffsetDateTime.now());
        }
        
        return doc;
    }

    @Override
    public int reindexUsers() {
        logger.info("Starting re-indexing of users from auth database...");
        int totalIndexed = 0;
        int errors = 0;

        try {
            // Query all users from auth database
            // Note: Table is in the 'users' table, schema is typically 'public' (default)
            String sql = "SELECT id, username, name, email, is_email_verified, created_at, updated_at " +
                        "FROM users " +
                        "ORDER BY created_at";

            logger.debug("Executing SQL query: {}", sql);
            List<Map<String, Object>> users = authJdbcTemplate.queryForList(sql);

            if (users.isEmpty()) {
                logger.warn("No users found in database. Check database connection and table name.");
                // Try to verify database connection
                try {
                    Integer count = authJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
                    logger.info("Total users in database: {}", count);
                } catch (Exception e) {
                    logger.error("Error checking user count: {}", e.getMessage(), e);
                }
            } else {
                logger.info("Found {} users to index", users.size());
            }

            for (Map<String, Object> userRow : users) {
                try {
                    UserDocument userDoc = convertRowToUserDocument(userRow);
                    indexingService.indexUser(userDoc);
                    totalIndexed++;
                } catch (Exception e) {
                    errors++;
                    Object userId = userRow.get("id");
                    logger.error("Error indexing user {}: {}", userId, e.getMessage(), e);
                }
            }

            logger.info("Re-indexing of users completed. Indexed: {}, Errors: {}", totalIndexed, errors);
        } catch (Exception e) {
            logger.error("Error during user re-indexing: {}", e.getMessage(), e);
        }

        return totalIndexed;
    }

    /**
     * Convert database row to UserDocument.
     */
    private UserDocument convertRowToUserDocument(Map<String, Object> row) {
        UserDocument doc = new UserDocument();
        
        // Convert UUID to String
        Object idObj = row.get("id");
        String id = idObj instanceof UUID ? idObj.toString() : idObj.toString();
        doc.setId(id);
        
        doc.setUsername((String) row.get("username"));
        doc.setDisplayName((String) row.get("name"));
        doc.setEmail((String) row.get("email"));
        
        // Convert Boolean
        Object isVerifiedObj = row.get("is_email_verified");
        Boolean isVerified = isVerifiedObj instanceof Boolean ? (Boolean) isVerifiedObj : 
                            (isVerifiedObj != null && isVerifiedObj.toString().equalsIgnoreCase("true"));
        doc.setIsVerified(isVerified);
        
        // Set defaults
        doc.setIsPrivate(false);
        doc.setFollowerCount(0);
        doc.setFollowingCount(0);
        doc.setBio(null);
        doc.setLocation(null);
        doc.setProfilePictureUrl(null);
        
        // Convert timestamps
        try {
            if (row.get("created_at") != null) {
                if (row.get("created_at") instanceof OffsetDateTime) {
                    doc.setCreatedAt((OffsetDateTime) row.get("created_at"));
                } else {
                    // Try to parse if it's a string or other format
                    doc.setCreatedAt(OffsetDateTime.now());
                }
            } else {
                doc.setCreatedAt(OffsetDateTime.now());
            }
            
            if (row.get("updated_at") != null) {
                if (row.get("updated_at") instanceof OffsetDateTime) {
                    doc.setUpdatedAt((OffsetDateTime) row.get("updated_at"));
                } else {
                    doc.setUpdatedAt(OffsetDateTime.now());
                }
            } else {
                doc.setUpdatedAt(OffsetDateTime.now());
            }
        } catch (Exception e) {
            logger.warn("Error parsing dates for user {}: {}", id, e.getMessage());
            OffsetDateTime now = OffsetDateTime.now();
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
        }
        
        return doc;
    }
}

