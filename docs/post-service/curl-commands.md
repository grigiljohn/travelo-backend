# Post Service - cURL Commands Reference

## Base URLs

- **Direct Service**: `http://localhost:8083`
- **Via API Gateway**: `http://localhost:8080/post-service`

---

## 📋 Table of Contents

1. [Posts Endpoints](#posts-endpoints)
   - [Create Post with Presigned S3 Uploads](#1-create-post-with-presigned-s3-uploads)
   - [Create Post with Media IDs](#2-create-post-with-media-ids)
   - [Get Posts](#3-get-posts)
   - [Get Post by ID](#4-get-post-by-id)
   - [Update Post](#5-update-post)
   - [Delete Post](#6-delete-post)
   - [Like Post](#7-like-post)
   - [Unlike Post](#8-unlike-post)
   - [Share Post](#9-share-post)

---

## 📝 Posts Endpoints

### Quick Reference: Multiple Files Flow

For posts with multiple files (carousel, mixed media), follow these steps:

1. **Request upload URLs** - One request per file
2. **Upload to S3** - Can be done in parallel
3. **Complete uploads** - One completion per file
4. **Create post** - Include all media IDs in `media_items` array

**Key Points:**
- Each file needs its own upload URL request
- Use `position` field to set order (0, 1, 2, ...)
- Set `post_type` to `"image"`, `"video"`, or `"mixed"` based on your files
- All files must be uploaded and completed before creating the post

---

### 1. Create Post with Presigned S3 Uploads

This is the recommended flow for creating posts with media files. The client uploads files directly to S3 using presigned URLs.

#### Step 1: Request Upload URLs

**POST** `/api/v1/posts/upload-urls`

Request presigned upload URLs for files. The client will upload files directly to S3 using these URLs.

```bash
curl -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "sunset.jpg",
    "mime_type": "image/jpeg",
    "size_bytes": 2048576,
    "media_type": "image",
    "resumable": false
  }'
```

**Request Body:**
```json
{
  "owner_id": "uuid",
  "filename": "filename.jpg",
  "mime_type": "image/jpeg|video/mp4|audio/mpeg",
  "size_bytes": 2048576,
  "media_type": "image|video|audio|other",
  "resumable": false
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Upload URL generated successfully",
  "data": {
    "media_id": "550e8400-e29b-41d4-a716-446655440000",
    "upload_method": "s3_presigned_put",
    "expires_in": 3600,
    "upload_url": "https://s3.amazonaws.com/bucket/key?presigned-params",
    "storage_key": "media/550e8400-e29b-41d4-a716-446655440000/original",
    "upload_id": null,
    "part_size": null,
    "presigned_part_urls": null
  }
}
```

**Handling Multiple Files:**

When creating a post with multiple files (e.g., a carousel of images or mixed media), you need to:

1. Request upload URLs for each file separately
2. Upload each file to S3
3. Complete each upload
4. Create the post with all media IDs

**Example: Multiple Images (Carousel Post)**

```bash
# Step 1: Request upload URLs for each image
# Image 1
RESPONSE1=$(curl -s -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "image1.jpg",
    "mime_type": "image/jpeg",
    "size_bytes": 1024000,
    "media_type": "image"
  }')
MEDIA_ID1=$(echo $RESPONSE1 | jq -r '.data.media_id')
UPLOAD_URL1=$(echo $RESPONSE1 | jq -r '.data.upload_url')

# Image 2
RESPONSE2=$(curl -s -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "image2.jpg",
    "mime_type": "image/jpeg",
    "size_bytes": 1536000,
    "media_type": "image"
  }')
MEDIA_ID2=$(echo $RESPONSE2 | jq -r '.data.media_id')
UPLOAD_URL2=$(echo $RESPONSE2 | jq -r '.data.upload_url')

# Image 3
RESPONSE3=$(curl -s -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "image3.jpg",
    "mime_type": "image/jpeg",
    "size_bytes": 2048000,
    "media_type": "image"
  }')
MEDIA_ID3=$(echo $RESPONSE3 | jq -r '.data.media_id')
UPLOAD_URL3=$(echo $RESPONSE3 | jq -r '.data.upload_url')

# Step 2: Upload all files to S3 (can be done in parallel)
curl -X PUT "$UPLOAD_URL1" -H "Content-Type: image/jpeg" --data-binary @/path/to/image1.jpg &
curl -X PUT "$UPLOAD_URL2" -H "Content-Type: image/jpeg" --data-binary @/path/to/image2.jpg &
curl -X PUT "$UPLOAD_URL3" -H "Content-Type: image/jpeg" --data-binary @/path/to/image3.jpg &
wait  # Wait for all uploads to complete

# Step 3: Complete all uploads
curl -X POST "http://localhost:8083/api/v1/posts/upload/complete/$MEDIA_ID1" \
  -H "Content-Type: application/json" \
  -d '{"etag": "\"etag1\"", "size_bytes": 1024000}' &
curl -X POST "http://localhost:8083/api/v1/posts/upload/complete/$MEDIA_ID2" \
  -H "Content-Type: application/json" \
  -d '{"etag": "\"etag2\"", "size_bytes": 1536000}' &
curl -X POST "http://localhost:8083/api/v1/posts/upload/complete/$MEDIA_ID3" \
  -H "Content-Type: application/json" \
  -d '{"etag": "\"etag3\"", "size_bytes": 2048000}' &
wait

# Step 4: Create post with all media IDs
curl -X POST http://localhost:8083/api/v1/posts \
  -H "Content-Type: application/json" \
  -d "{
    \"post_type\": \"image\",
    \"mood\": \"adventure\",
    \"caption\": \"Amazing travel photos!\",
    \"media_items\": [
      {\"media_id\": \"$MEDIA_ID1\", \"type\": \"image\", \"position\": 0},
      {\"media_id\": \"$MEDIA_ID2\", \"type\": \"image\", \"position\": 1},
      {\"media_id\": \"$MEDIA_ID3\", \"type\": \"image\", \"position\": 2}
    ]
  }"
```

**Example: Mixed Media (Images + Video)**

```bash
# Request upload URL for image
IMAGE_RESPONSE=$(curl -s -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "thumbnail.jpg",
    "mime_type": "image/jpeg",
    "size_bytes": 512000,
    "media_type": "image"
  }')
IMAGE_MEDIA_ID=$(echo $IMAGE_RESPONSE | jq -r '.data.media_id')
IMAGE_UPLOAD_URL=$(echo $IMAGE_RESPONSE | jq -r '.data.upload_url')

# Request upload URL for video
VIDEO_RESPONSE=$(curl -s -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -d '{
    "owner_id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "video.mp4",
    "mime_type": "video/mp4",
    "size_bytes": 15728640,
    "media_type": "video",
    "resumable": true
  }')
VIDEO_MEDIA_ID=$(echo $VIDEO_RESPONSE | jq -r '.data.media_id')
VIDEO_UPLOAD_URL=$(echo $VIDEO_RESPONSE | jq -r '.data.upload_url')

# Upload both files
curl -X PUT "$IMAGE_UPLOAD_URL" -H "Content-Type: image/jpeg" --data-binary @/path/to/thumbnail.jpg
curl -X PUT "$VIDEO_UPLOAD_URL" -H "Content-Type: video/mp4" --data-binary @/path/to/video.mp4

# Complete uploads
curl -X POST "http://localhost:8083/api/v1/posts/upload/complete/$IMAGE_MEDIA_ID" \
  -H "Content-Type: application/json" \
  -d '{"etag": "\"image-etag\"", "size_bytes": 512000}'
curl -X POST "http://localhost:8083/api/v1/posts/upload/complete/$VIDEO_MEDIA_ID" \
  -H "Content-Type: application/json" \
  -d '{"etag": "\"video-etag\"", "size_bytes": 15728640}'

# Create post with mixed media (use "mixed" post_type)
curl -X POST http://localhost:8083/api/v1/posts \
  -H "Content-Type: application/json" \
  -d "{
    \"post_type\": \"mixed\",
    \"mood\": \"adventure\",
    \"caption\": \"Check out this amazing video!\",
    \"media_items\": [
      {\"media_id\": \"$IMAGE_MEDIA_ID\", \"type\": \"image\", \"position\": 0},
      {\"media_id\": \"$VIDEO_MEDIA_ID\", \"type\": \"video\", \"position\": 1}
    ]
  }"
```

**Important Notes for Multiple Files:**

1. **Post Type Selection:**
   - `"image"` - Use when all files are images
   - `"video"` - Use when all files are videos
   - `"mixed"` - Use when you have both images and videos

2. **Position Field:**
   - The `position` field in `media_items` determines the order of files in the carousel
   - Start from 0 and increment for each file
   - Example: `[0, 1, 2]` for a 3-image carousel

3. **Parallel Uploads:**
   - You can upload multiple files to S3 in parallel to speed up the process
   - Use background processes (`&`) and `wait` in bash scripts

4. **Error Handling:**
   - If one file upload fails, you can retry just that file
   - Only create the post after all files are successfully uploaded and completed

#### Step 2: Upload File to S3

Upload the file directly to S3 using the presigned URL from Step 1.

```bash
# For single file upload (PUT request)
curl -X PUT "https://s3.amazonaws.com/bucket/key?presigned-params" \
  -H "Content-Type: image/jpeg" \
  --data-binary @/path/to/sunset.jpg
```

**Important:** 
- Use the exact `Content-Type` header that matches the `mime_type` from Step 1
- Use PUT method for single file uploads
- Include the file as binary data
- The presigned URL includes all necessary authentication parameters

**Example with Response Headers:**
```bash
curl -X PUT "https://s3.amazonaws.com/bucket/key?presigned-params" \
  -H "Content-Type: image/jpeg" \
  -H "Content-Length: 2048576" \
  --data-binary @/path/to/sunset.jpg \
  -v
```

**For Large Files (Resumable Upload):**
If `resumable: true` was set, you'll receive `presigned_part_urls` in the response. Use multipart upload:

```bash
# Upload each part separately
curl -X PUT "https://s3.amazonaws.com/bucket/key?partNumber=1&uploadId=xxx" \
  -H "Content-Type: video/mp4" \
  --data-binary @/path/to/video-part1.mp4

curl -X PUT "https://s3.amazonaws.com/bucket/key?partNumber=2&uploadId=xxx" \
  -H "Content-Type: video/mp4" \
  --data-binary @/path/to/video-part2.mp4
```

#### Step 3: Confirm Upload Completion

**POST** `/api/v1/posts/upload/complete/{mediaId}`

Confirm that the upload to S3 is complete. This triggers media processing and returns the download URL.

```bash
curl -X POST http://localhost:8083/api/v1/posts/upload/complete/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "etag": "\"d41d8cd98f00b204e9800998ecf8427e\"",
    "size_bytes": 2048576
  }'
```

**Request Body:**
```json
{
  "etag": "\"etag-from-s3-response\"",
  "size_bytes": 2048576
}
```

**Note:** The ETag is returned in the S3 PUT response headers. For multipart uploads, you'll need to combine ETags.

**Response (200):**
```json
{
  "success": true,
  "message": "Upload completed successfully",
  "data": {
    "media_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "completed",
    "download_url": "https://s3.amazonaws.com/bucket/key?presigned-download-url"
  }
}
```

#### Step 4: Create Post with Media IDs

**POST** `/api/v1/posts` (application/json)

Create the post using the media IDs from Step 3.

```bash
curl -X POST http://localhost:8083/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{
    "post_type": "image",
    "mood": "adventure",
    "caption": "Beautiful sunset at the beach! 🌅",
    "location": "Bali, Indonesia",
    "tags": ["travel", "beach", "sunset"],
    "media_items": [
      {
        "media_id": "550e8400-e29b-41d4-a716-446655440000",
        "type": "image",
        "position": 0
      }
    ]
  }'
```

**Request Body:**
```json
{
  "post_type": "image|video|reel|text|mixed",
  "mood": "chill|love|adventure|party|nature|food|culture|romantic|activity|relax",
  "caption": "Post caption",
  "location": "Location string",
  "tags": ["tag1", "tag2"],
  "media_items": [
    {
      "media_id": "uuid",
      "type": "image|video",
      "position": 0,
      "thumbnail_url": "optional",
      "duration": 120,
      "width": 1920,
      "height": 1080
    },
    {
      "media_id": "uuid",
      "type": "image|video",
      "position": 1
    }
  ]
}
```

**Post Type Guidelines:**
- `"image"` - All media items are images (carousel of photos)
- `"video"` - All media items are videos
- `"mixed"` - Combination of images and videos
- `"reel"` - Short-form video content
- `"text"` - Text-only post (no media_items required)

**Multiple Media Items Example:**
```json
{
  "post_type": "image",
  "mood": "adventure",
  "caption": "Amazing travel journey!",
  "media_items": [
    {"media_id": "uuid-1", "type": "image", "position": 0},
    {"media_id": "uuid-2", "type": "image", "position": 1},
    {"media_id": "uuid-3", "type": "image", "position": 2}
  ]
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "post-uuid",
    "caption": "Beautiful sunset at the beach! 🌅",
    "mood": "adventure",
    "location": "Bali, Indonesia",
    "tags": ["travel", "beach", "sunset"],
    "mediaItems": [
      {
        "id": 1,
        "mediaUrl": "https://s3.amazonaws.com/bucket/key?presigned-url",
        "thumbnailUrl": "https://s3.amazonaws.com/bucket/key-thumb?presigned-url",
        "mediaType": "IMAGE",
        "orderIndex": 0,
        "createdAt": "2025-12-09T10:00:00Z"
      }
    ],
    "likes": 0,
    "shares": 0,
    "comments": 0,
    "createdAt": "2025-12-09T10:00:00Z"
  }
}
```

**Complete Example Flow (Bash Script):**

```bash
#!/bin/bash

# Configuration
OWNER_ID="550e8400-e29b-41d4-a716-446655440000"
BASE_URL="http://localhost:8083"
FILE_PATH="/path/to/sunset.jpg"
FILE_NAME="sunset.jpg"
MIME_TYPE="image/jpeg"
FILE_SIZE=$(stat -f%z "$FILE_PATH" 2>/dev/null || stat -c%s "$FILE_PATH" 2>/dev/null)

# Step 1: Request upload URL
echo "Step 1: Requesting upload URL..."
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/posts/upload-urls" \
  -H "Content-Type: application/json" \
  -d "{
    \"owner_id\": \"$OWNER_ID\",
    \"filename\": \"$FILE_NAME\",
    \"mime_type\": \"$MIME_TYPE\",
    \"size_bytes\": $FILE_SIZE,
    \"media_type\": \"image\"
  }")

# Extract media_id and upload_url
MEDIA_ID=$(echo $UPLOAD_RESPONSE | jq -r '.data.media_id')
UPLOAD_URL=$(echo $UPLOAD_RESPONSE | jq -r '.data.upload_url')

echo "Media ID: $MEDIA_ID"
echo "Upload URL: $UPLOAD_URL"

# Step 2: Upload file to S3
echo "Step 2: Uploading file to S3..."
S3_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$UPLOAD_URL" \
  -H "Content-Type: $MIME_TYPE" \
  --data-binary @"$FILE_PATH")

HTTP_CODE=$(echo "$S3_RESPONSE" | tail -n1)
ETAG=$(echo "$S3_RESPONSE" | grep -i "etag" | sed 's/.*ETag: *"\([^"]*\)".*/\1/')

if [ "$HTTP_CODE" != "200" ]; then
  echo "Upload failed with HTTP code: $HTTP_CODE"
  exit 1
fi

echo "Upload successful. ETag: $ETAG"

# Step 3: Confirm upload
echo "Step 3: Confirming upload..."
COMPLETE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/posts/upload/complete/$MEDIA_ID" \
  -H "Content-Type: application/json" \
  -d "{
    \"etag\": \"\\\"$ETAG\\\"\",
    \"size_bytes\": $FILE_SIZE
  }")

DOWNLOAD_URL=$(echo $COMPLETE_RESPONSE | jq -r '.data.download_url')
echo "Download URL: $DOWNLOAD_URL"

# Step 4: Create post
echo "Step 4: Creating post..."
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/posts" \
  -H "Content-Type: application/json" \
  -d "{
    \"post_type\": \"image\",
    \"mood\": \"adventure\",
    \"caption\": \"Beautiful sunset!\",
    \"location\": \"Bali, Indonesia\",
    \"tags\": [\"travel\", \"beach\", \"sunset\"],
    \"media_items\": [
      {
        \"media_id\": \"$MEDIA_ID\",
        \"type\": \"image\",
        \"position\": 0
      }
    ]
  }")

POST_ID=$(echo $POST_RESPONSE | jq -r '.data.id')
echo "Post created successfully! Post ID: $POST_ID"
echo "Full response:"
echo $POST_RESPONSE | jq '.'
```

**Complete Example: Multiple Files Script (Production Ready)**

```bash
#!/bin/bash

# Configuration
OWNER_ID="550e8400-e29b-41d4-a716-446655440000"
BASE_URL="http://localhost:8083"

# Array of files: "file_path:mime_type:media_type"
declare -a FILES=(
  "/path/to/image1.jpg:image/jpeg:image"
  "/path/to/image2.jpg:image/jpeg:image"
  "/path/to/image3.jpg:image/jpeg:image"
  "/path/to/video.mp4:video/mp4:video"
)

declare -a MEDIA_IDS=()
declare -a MEDIA_TYPES=()
declare -a FILE_SIZES=()

echo "=== Step 1: Requesting Upload URLs ==="
# Step 1: Request upload URLs for all files
for FILE_INFO in "${FILES[@]}"; do
  IFS=':' read -r FILE_PATH MIME_TYPE MEDIA_TYPE <<< "$FILE_INFO"
  FILE_NAME=$(basename "$FILE_PATH")
  FILE_SIZE=$(stat -f%z "$FILE_PATH" 2>/dev/null || stat -c%s "$FILE_PATH" 2>/dev/null)
  
  echo "Requesting URL for $FILE_NAME..."
  
  UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/posts/upload-urls" \
    -H "Content-Type: application/json" \
    -d "{
      \"owner_id\": \"$OWNER_ID\",
      \"filename\": \"$FILE_NAME\",
      \"mime_type\": \"$MIME_TYPE\",
      \"size_bytes\": $FILE_SIZE,
      \"media_type\": \"$MEDIA_TYPE\"
    }")
  
  MEDIA_ID=$(echo $UPLOAD_RESPONSE | jq -r '.data.media_id')
  UPLOAD_URL=$(echo $UPLOAD_RESPONSE | jq -r '.data.upload_url')
  
  if [ "$MEDIA_ID" == "null" ] || [ -z "$MEDIA_ID" ]; then
    echo "Error: Failed to get upload URL for $FILE_NAME"
    exit 1
  fi
  
  MEDIA_IDS+=("$MEDIA_ID")
  MEDIA_TYPES+=("$MEDIA_TYPE")
  FILE_SIZES+=("$FILE_SIZE")
  
  # Store upload URL for later use
  eval "UPLOAD_URL_${#MEDIA_IDS[@]}=\"$UPLOAD_URL\""
  eval "MIME_TYPE_${#MEDIA_IDS[@]}=\"$MIME_TYPE\""
  eval "FILE_PATH_${#MEDIA_IDS[@]}=\"$FILE_PATH\""
  
  echo "  Media ID: $MEDIA_ID"
done

echo ""
echo "=== Step 2: Uploading Files to S3 (Parallel) ==="
# Step 2: Upload all files to S3 in parallel
for i in "${!MEDIA_IDS[@]}"; do
  idx=$((i + 1))
  eval "UPLOAD_URL=\$UPLOAD_URL_$idx"
  eval "MIME_TYPE=\$MIME_TYPE_$idx"
  eval "FILE_PATH=\$FILE_PATH_$idx"
  
  echo "Uploading file $idx to S3..."
  curl -s -X PUT "$UPLOAD_URL" \
    -H "Content-Type: $MIME_TYPE" \
    --data-binary @"$FILE_PATH" > /dev/null &
done
wait
echo "All files uploaded successfully!"

echo ""
echo "=== Step 3: Completing Uploads ==="
# Step 3: Complete all uploads
for i in "${!MEDIA_IDS[@]}"; do
  MEDIA_ID="${MEDIA_IDS[$i]}"
  FILE_SIZE="${FILE_SIZES[$i]}"
  
  echo "Completing upload for media $((i + 1))..."
  curl -s -X POST "$BASE_URL/api/v1/posts/upload/complete/$MEDIA_ID" \
    -H "Content-Type: application/json" \
    -d "{
      \"etag\": \"\\\"placeholder\\\"\",
      \"size_bytes\": $FILE_SIZE
    }" > /dev/null
done
echo "All uploads completed!"

echo ""
echo "=== Step 4: Creating Post ==="
# Step 4: Build media_items array
MEDIA_ITEMS=""
POST_TYPE="image"  # Default, will be determined below

# Determine post type based on media types
HAS_IMAGE=false
HAS_VIDEO=false
for TYPE in "${MEDIA_TYPES[@]}"; do
  if [ "$TYPE" == "image" ]; then
    HAS_IMAGE=true
  elif [ "$TYPE" == "video" ]; then
    HAS_VIDEO=true
  fi
done

if [ "$HAS_IMAGE" == true ] && [ "$HAS_VIDEO" == true ]; then
  POST_TYPE="mixed"
elif [ "$HAS_VIDEO" == true ]; then
  POST_TYPE="video"
fi

# Build media_items JSON array
for i in "${!MEDIA_IDS[@]}"; do
  if [ $i -gt 0 ]; then
    MEDIA_ITEMS+=","
  fi
  MEDIA_ITEMS+="{\"media_id\": \"${MEDIA_IDS[$i]}\", \"type\": \"${MEDIA_TYPES[$i]}\", \"position\": $i}"
done

# Create post
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/posts" \
  -H "Content-Type: application/json" \
  -d "{
    \"post_type\": \"$POST_TYPE\",
    \"mood\": \"adventure\",
    \"caption\": \"Amazing travel experience with ${#MEDIA_IDS[@]} files!\",
    \"location\": \"Bali, Indonesia\",
    \"tags\": [\"travel\", \"adventure\"],
    \"media_items\": [$MEDIA_ITEMS]
  }")

POST_ID=$(echo $POST_RESPONSE | jq -r '.data.id')
if [ "$POST_ID" != "null" ] && [ -n "$POST_ID" ]; then
  echo "✅ Post created successfully!"
  echo "   Post ID: $POST_ID"
  echo "   Post Type: $POST_TYPE"
  echo "   Media Count: ${#MEDIA_IDS[@]}"
  echo ""
  echo "Full response:"
  echo $POST_RESPONSE | jq '.'
else
  echo "❌ Failed to create post"
  echo $POST_RESPONSE | jq '.'
  exit 1
fi
```

---

### 2. Create Post with Media IDs

**POST** `/api/v1/posts` (application/json)

Create a post using existing media IDs (if you already have media uploaded).

```bash
curl -X POST http://localhost:8083/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{
    "post_type": "image",
    "mood": "adventure",
    "caption": "Amazing travel experience!",
    "location": "Paris, France",
    "tags": ["travel", "paris"],
    "media_items": [
      {
        "media_id": "550e8400-e29b-41d4-a716-446655440000",
        "type": "image",
        "position": 0
      }
    ]
  }'
```

---

### 3. Get Posts

**GET** `/api/v1/posts`

Retrieve a paginated list of posts.

```bash
# Get first page (default: 10 posts)
curl -X GET "http://localhost:8083/api/v1/posts"

# Get specific page with limit
curl -X GET "http://localhost:8083/api/v1/posts?page=1&limit=20"

# Filter by mood
curl -X GET "http://localhost:8083/api/v1/posts?mood=adventure&page=1&limit=10"
```

**Query Parameters:**
- `page` (optional, default: 1): Page number
- `limit` (optional, default: 10, max: 50): Number of posts per page
- `mood` (optional): Filter by mood type

**Response (200):**
```json
{
  "success": true,
  "message": "Posts retrieved successfully",
  "data": {
    "data": [...],
    "page": 1,
    "limit": 10,
    "total": 100,
    "totalPages": 10
  }
}
```

---

### 4. Get Post by ID

**GET** `/api/v1/posts/{postId}`

Retrieve a specific post by its ID.

```bash
curl -X GET "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000"
```

**Response (200):**
```json
{
  "success": true,
  "message": "Post retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "caption": "Beautiful sunset!",
    "mood": "adventure",
    "mediaItems": [...],
    "likes": 42,
    "shares": 5,
    "comments": 12,
    "createdAt": "2025-12-09T10:00:00Z"
  }
}
```

---

### 5. Update Post

**PATCH** `/api/v1/posts/{postId}`

Update an existing post.

```bash
curl -X PATCH "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "caption": "Updated caption",
    "location": "New location",
    "tags": ["new", "tags"]
  }'
```

**Request Body (all fields optional):**
```json
{
  "caption": "Updated caption",
  "location": "New location",
  "tags": ["tag1", "tag2"]
}
```

---

### 6. Delete Post

**DELETE** `/api/v1/posts/{postId}`

Delete a post (soft delete).

```bash
curl -X DELETE "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000"
```

**Response (200):**
```json
{
  "success": true,
  "message": "Post deleted successfully",
  "data": null
}
```

---

### 7. Like Post

**POST** `/api/v1/posts/{postId}/like`

Like a post.

```bash
# Like post
curl -X POST "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000/like" \
  -H "Content-Type: application/json" \
  -d '{
    "liked": true
  }'

# Unlike post (set liked to false)
curl -X POST "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000/like" \
  -H "Content-Type: application/json" \
  -d '{
    "liked": false
  }'
```

---

### 8. Unlike Post

**DELETE** `/api/v1/posts/{postId}/like`

Unlike a post.

```bash
curl -X DELETE "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000/like"
```

---

### 9. Share Post

**POST** `/api/v1/posts/{postId}/share`

Share a post.

```bash
curl -X POST "http://localhost:8083/api/v1/posts/550e8400-e29b-41d4-a716-446655440000/share" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "facebook"
  }'
```

**Response (200):**
```json
{
  "success": true,
  "message": "Post shared successfully",
  "data": {
    "shares": 6,
    "shareUrl": "https://your-app.com/posts/550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## 🔐 Authentication

If authentication is required, include the JWT token in the Authorization header:

```bash
curl -X POST http://localhost:8083/api/v1/posts/upload-urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{...}'
```

---

## 📝 Notes

1. **Presigned URLs expire**: Upload URLs typically expire after 1 hour. Upload files promptly after receiving the URL.

2. **ETag for completion**: For single file uploads, the ETag is in the S3 response headers. For multipart uploads, you need to combine ETags.

3. **Media processing**: After completing upload, media processing (thumbnails, transcoding) happens asynchronously. The download URL may not be immediately available.

4. **File size limits**: Check your S3 bucket configuration for maximum file size limits.

5. **Content-Type matching**: The Content-Type header in the S3 upload must match the `mime_type` specified when requesting the upload URL.

---

## 🐛 Error Responses

All endpoints return errors in the following format:

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "errors": ["Detailed error messages"]
}
```

**Common HTTP Status Codes:**
- `200 OK`: Success
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

