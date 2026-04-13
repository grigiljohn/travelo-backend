package com.travelo.searchservice.service;

import com.travelo.searchservice.document.PostDocument;
import com.travelo.searchservice.document.UserDocument;
import com.travelo.searchservice.document.HashtagDocument;
import com.travelo.searchservice.document.LocationDocument;
import com.travelo.searchservice.document.ShopDocument;
import com.travelo.searchservice.document.ProductDocument;

/**
 * Service for indexing documents in Elasticsearch.
 */
public interface SearchIndexingService {
    
    /**
     * Index or update a post document.
     */
    void indexPost(PostDocument post);
    
    /**
     * Delete a post from index.
     */
    void deletePost(String postId);
    
    /**
     * Index or update a user document.
     */
    void indexUser(UserDocument user);
    
    /**
     * Delete a user from index.
     */
    void deleteUser(String userId);
    
    /**
     * Index or update a hashtag document.
     */
    void indexHashtag(HashtagDocument hashtag);
    
    /**
     * Delete a hashtag from index.
     */
    void deleteHashtag(String hashtagId);
    
    /**
     * Index or update a location document.
     */
    void indexLocation(LocationDocument location);
    
    /**
     * Delete a location from index.
     */
    void deleteLocation(String locationId);
    
    /**
     * Index or update a shop document.
     */
    void indexShop(ShopDocument shop);
    
    /**
     * Delete a shop from index.
     */
    void deleteShop(String shopId);
    
    /**
     * Index or update a product document.
     */
    void indexProduct(ProductDocument product);
    
    /**
     * Delete a product from index.
     */
    void deleteProduct(String productId);
}

