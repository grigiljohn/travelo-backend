# Re-indexing Utility Guide

## Overview

The re-indexing utility allows you to manually sync data from source services (post-service, shop-service) to Elasticsearch. This is useful when:
- Data was added directly to the database (e.g., via scripts)
- Events were missed or failed
- You need to rebuild the search index
- You want to sync existing data after setting up the search-service

## API Endpoints

All endpoints are under `/api/v1/admin/reindex`:

**Note:** For user reindexing, ensure the auth database connection is configured in `application.yml`:
```yaml
app:
  auth-db:
    url: jdbc:postgresql://localhost:5432/travelo_auth
    username: travelo
    password: travelo
```

### 1. Re-index All Reels
```bash
POST http://localhost:8087/api/v1/admin/reindex/reels
```

Re-indexes only posts with `postType = 'reel'` from post-service.

**Response:**
```json
{
  "success": true,
  "message": "Reels re-indexed successfully",
  "reelsIndexed": 20
}
```

### 2. Re-index All Posts
```bash
POST http://localhost:8087/api/v1/admin/reindex/posts
```

Re-indexes all posts (including reels) from post-service.

**Response:**
```json
{
  "success": true,
  "message": "Posts re-indexed successfully",
  "postsIndexed": 150
}
```

### 3. Re-index All Shops
```bash
POST http://localhost:8087/api/v1/admin/reindex/shops
```

Re-indexes all shops from shop-service.

**Response:**
```json
{
  "success": true,
  "message": "Shops re-indexed successfully",
  "shopsIndexed": 10
}
```

### 4. Re-index All Products
```bash
POST http://localhost:8087/api/v1/admin/reindex/products
```

Re-indexes all products from all shops in shop-service.

**Response:**
```json
{
  "success": true,
  "message": "Products re-indexed successfully",
  "productsIndexed": 250
}
```

### 5. Re-index All Users
```bash
POST http://localhost:8087/api/v1/admin/reindex/users
```

Re-indexes all users from auth-service database (PostgreSQL) to Elasticsearch. This is useful when users exist in the database but haven't been indexed yet.

**Response:**
```json
{
  "success": true,
  "message": "Users re-indexed successfully",
  "usersIndexed": 25
}
```

### 6. Re-index Everything
```bash
POST http://localhost:8087/api/v1/admin/reindex/all
```

Re-indexes all data (posts, shops, products) in one operation.

**Response:**
```json
{
  "success": true,
  "message": "Re-indexing completed. Posts: 150, Shops: 10, Products: 250",
  "postsIndexed": 150,
  "shopsIndexed": 10,
  "productsIndexed": 250,
  "errors": 0
}
```

## Usage Examples

### Using cURL

```bash
# Re-index your 20 reels
curl -X POST http://localhost:8087/api/v1/admin/reindex/reels

# Re-index all posts
curl -X POST http://localhost:8087/api/v1/admin/reindex/posts

# Re-index all shops and products
curl -X POST http://localhost:8087/api/v1/admin/reindex/shops
curl -X POST http://localhost:8087/api/v1/admin/reindex/products

# Re-index all users
curl -X POST http://localhost:8087/api/v1/admin/reindex/users

# Re-index everything
curl -X POST http://localhost:8087/api/v1/admin/reindex/all
```

### Using Postman

1. Set method to `POST`
2. Set URL to `http://localhost:8087/api/v1/admin/reindex/reels` (or other endpoint)
3. Click Send

## How It Works

1. **Fetches Data**: The service calls the source service APIs (post-service, shop-service) with pagination
2. **Converts Data**: Transforms DTOs from source services into Elasticsearch documents
3. **Indexes Data**: Saves documents to Elasticsearch via `SearchIndexingService`
4. **Batch Processing**: Processes data in batches of 50 items at a time for efficiency
5. **Error Handling**: Continues processing even if individual items fail, logs errors

## Important Notes

- **Batch Size**: Processes 50 items per batch to avoid memory issues
- **Pagination**: Automatically handles pagination until all data is processed
- **Error Resilience**: Individual item failures don't stop the entire process
- **Idempotent**: Safe to run multiple times - will update existing documents
- **Performance**: Large datasets may take several minutes to process

## Troubleshooting

### No data indexed
- Check that source services (post-service, shop-service) are running
- Verify Elasticsearch is running and accessible
- Check service logs for errors

### Partial indexing
- Check logs for specific item errors
- Re-run the endpoint - it's safe to run multiple times
- Verify data exists in source service databases

### Timeout errors
- For large datasets, consider running individual endpoints instead of `/all`
- Increase timeout settings if needed
- Process in smaller batches by modifying `BATCH_SIZE` in `ReindexingServiceImpl`

## Security Note

⚠️ **These are admin endpoints** - In production, you should:
- Add authentication/authorization
- Restrict access to admin users only
- Consider rate limiting
- Add IP whitelisting if needed

## Architecture

```
ReindexingController
    ↓
ReindexingService
    ↓
PostServiceClient / ShopServiceClient
    ↓
Source Services (post-service, shop-service)
    ↓
ReindexingService (converts DTOs to Documents)
    ↓
SearchIndexingService
    ↓
Elasticsearch
```

