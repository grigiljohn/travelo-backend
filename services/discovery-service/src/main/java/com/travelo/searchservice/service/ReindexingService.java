package com.travelo.searchservice.service;

/**
 * Service for re-indexing data from source services to Elasticsearch.
 */
public interface ReindexingService {
    
    /**
     * Re-index all posts/reels from post-service.
     * @return Number of posts indexed
     */
    int reindexPosts();
    
    /**
     * Re-index all shops from shop-service.
     * @return Number of shops indexed
     */
    int reindexShops();
    
    /**
     * Re-index all products from shop-service.
     * @return Number of products indexed
     */
    int reindexProducts();
    
    /**
     * Re-index all data (posts, shops, products).
     * @return Summary of re-indexing results
     */
    ReindexingResult reindexAll();
    
    /**
     * Re-index only reels (posts with postType = 'reel').
     * @return Number of reels indexed
     */
    int reindexReels();
    
    /**
     * Re-index all users from auth-service database to Elasticsearch.
     * @return Number of users indexed
     */
    int reindexUsers();
    
    /**
     * Result of re-indexing operation.
     */
    class ReindexingResult {
        private int postsIndexed;
        private int shopsIndexed;
        private int productsIndexed;
        private int errors;
        private String message;
        
        public ReindexingResult(int postsIndexed, int shopsIndexed, int productsIndexed, int errors, String message) {
            this.postsIndexed = postsIndexed;
            this.shopsIndexed = shopsIndexed;
            this.productsIndexed = productsIndexed;
            this.errors = errors;
            this.message = message;
        }
        
        // Getters
        public int getPostsIndexed() { return postsIndexed; }
        public int getShopsIndexed() { return shopsIndexed; }
        public int getProductsIndexed() { return productsIndexed; }
        public int getErrors() { return errors; }
        public String getMessage() { return message; }
    }
}

