package com.travelo.searchservice.service.impl;

import com.travelo.searchservice.document.*;
import com.travelo.searchservice.repository.*;
import com.travelo.searchservice.service.SearchIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexingServiceImpl implements SearchIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingServiceImpl.class);

    private final PostDocumentRepository postRepository;
    private final UserDocumentRepository userRepository;
    private final HashtagDocumentRepository hashtagRepository;
    private final LocationDocumentRepository locationRepository;
    private final ShopDocumentRepository shopRepository;
    private final ProductDocumentRepository productRepository;

    public SearchIndexingServiceImpl(
            @Lazy PostDocumentRepository postRepository,
            @Lazy UserDocumentRepository userRepository,
            @Lazy HashtagDocumentRepository hashtagRepository,
            @Lazy LocationDocumentRepository locationRepository,
            @Lazy ShopDocumentRepository shopRepository,
            @Lazy ProductDocumentRepository productRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.hashtagRepository = hashtagRepository;
        this.locationRepository = locationRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void indexPost(PostDocument post) {
        try {
            postRepository.save(post);
            logger.debug("Indexed post: {}", post.getId());
        } catch (Exception e) {
            logger.error("Error indexing post {}: {}", post.getId(), e.getMessage());
        }
    }

    @Override
    public void deletePost(String postId) {
        try {
            postRepository.deleteById(postId);
            logger.debug("Deleted post from index: {}", postId);
        } catch (Exception e) {
            logger.error("Error deleting post {} from index: {}", postId, e.getMessage());
        }
    }

    @Override
    public void indexUser(UserDocument user) {
        try {
            userRepository.save(user);
            logger.debug("Indexed user: {}", user.getId());
        } catch (Exception e) {
            logger.error("Error indexing user {}: {}", user.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteUser(String userId) {
        try {
            userRepository.deleteById(userId);
            logger.debug("Deleted user from index: {}", userId);
        } catch (Exception e) {
            logger.error("Error deleting user {} from index: {}", userId, e.getMessage());
        }
    }

    @Override
    public void indexHashtag(HashtagDocument hashtag) {
        try {
            hashtagRepository.save(hashtag);
            logger.debug("Indexed hashtag: {}", hashtag.getTag());
        } catch (Exception e) {
            logger.error("Error indexing hashtag {}: {}", hashtag.getTag(), e.getMessage());
        }
    }

    @Override
    public void deleteHashtag(String hashtagId) {
        try {
            hashtagRepository.deleteById(hashtagId);
            logger.debug("Deleted hashtag from index: {}", hashtagId);
        } catch (Exception e) {
            logger.error("Error deleting hashtag {} from index: {}", hashtagId, e.getMessage());
        }
    }

    @Override
    public void indexLocation(LocationDocument location) {
        try {
            locationRepository.save(location);
            logger.debug("Indexed location: {}", location.getName());
        } catch (Exception e) {
            logger.error("Error indexing location {}: {}", location.getName(), e.getMessage());
        }
    }

    @Override
    public void deleteLocation(String locationId) {
        try {
            locationRepository.deleteById(locationId);
            logger.debug("Deleted location from index: {}", locationId);
        } catch (Exception e) {
            logger.error("Error deleting location {} from index: {}", locationId, e.getMessage());
        }
    }

    @Override
    public void indexShop(ShopDocument shop) {
        try {
            shopRepository.save(shop);
            logger.debug("Indexed shop: {}", shop.getId());
        } catch (Exception e) {
            logger.error("Error indexing shop {}: {}", shop.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteShop(String shopId) {
        try {
            shopRepository.deleteById(shopId);
            logger.debug("Deleted shop from index: {}", shopId);
        } catch (Exception e) {
            logger.error("Error deleting shop {} from index: {}", shopId, e.getMessage());
        }
    }

    @Override
    public void indexProduct(ProductDocument product) {
        try {
            productRepository.save(product);
            logger.debug("Indexed product: {}", product.getId());
        } catch (Exception e) {
            logger.error("Error indexing product {}: {}", product.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteProduct(String productId) {
        try {
            productRepository.deleteById(productId);
            logger.debug("Deleted product from index: {}", productId);
        } catch (Exception e) {
            logger.error("Error deleting product {} from index: {}", productId, e.getMessage());
        }
    }
}

