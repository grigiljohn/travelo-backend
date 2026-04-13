# Create Post Flow - Complete Explanation

## Overview

The Travelo backend supports **two different flows** for creating posts with media files:

1. **Multipart Upload Flow** (Simpler, server handles everything)
2. **Presigned S3 Upload Flow** (Recommended, faster, more scalable)

---

## 📱 Flow 1: Multipart Upload (Simpler)

This is the **easiest flow** for mobile apps. The app sends files directly to the backend, and the backend handles all the S3 upload complexity.

### Mobile App Steps

1. **User selects files** (images/videos from gallery or camera)
2. **User fills post details** (caption, mood, location, tags)
3. **User taps "Post" button**
4. **App validates files** (size, type, count)
5. **App creates multipart request** with:
   - `files`: Array of image/video files
   - `caption`: Post caption text
   - `mood`: Mood type (chill, adventure, etc.)
   - `location`: Optional location string
   - `tags`: Comma-separated tags
   - `owner_id`: User's UUID
6. **App sends POST request** to `/api/v1/posts` with `Content-Type: multipart/form-data`
7. **App shows loading indicator** ("Publishing...")
8. **App receives response** with created post data
9. **App navigates to post** or shows success message

### Backend Steps (Automatic)

When the backend receives the multipart request:

1. **Extract files and metadata** from multipart form data
2. **For each file:**
   - Request presigned upload URL from media service
   - Upload file to S3 using presigned URL
   - Extract ETag from S3 response
   - Complete upload with media service
   - Get media ID
3. **Create media items** list with all media IDs
4. **Determine post type** (image/video/mixed) from media items
5. **Create post** in database with:
   - Post metadata (caption, mood, location, tags)
   - Media items linked to post
   - Post type determined automatically
6. **Fetch S3 URLs** for all media items from media service
7. **Return post data** with S3 URLs

### API Call Example

```http
POST /api/v1/posts
Content-Type: multipart/form-data
Authorization: Bearer <access_token>

files: [File1, File2, File3]
caption: "Amazing sunset!"
mood: "adventure"
location: "Bali, Indonesia"
tags: "travel,beach,sunset"
owner_id: "550e8400-e29b-41d4-a716-446655440000"
```

### Response

```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "post-123",
    "post_type": "image",
    "caption": "Amazing sunset!",
    "mood": "adventure",
    "media_items": [
      {
        "id": "1",
        "url": "https://s3.amazonaws.com/bucket/media-id-1/original.jpg",
        "type": "image",
        "position": 0
      }
    ]
  }
}
```

---

## 🚀 Flow 2: Presigned S3 Upload (Recommended)

This is the **recommended flow** for better performance and scalability. The mobile app uploads files directly to S3, reducing server load.

### Mobile App Steps

#### Step 1: User Selects Files
- User opens gallery/camera
- Selects one or more images/videos
- App validates files (size, type)

#### Step 2: Request Upload URLs
For each file, app makes a request:

```http
POST /api/v1/posts/upload-urls
Content-Type: application/json
Authorization: Bearer <access_token>

{
  "owner_id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "sunset.jpg",
  "mime_type": "image/jpeg",
  "size_bytes": 2048576,
  "media_type": "image",
  "resumable": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "media_id": "550e8400-e29b-41d4-a716-446655440000",
    "upload_url": "https://s3.amazonaws.com/bucket/key?presigned-params",
    "expires_in": 3600
  }
}
```

**App State:** Show "Preparing upload..." indicator

#### Step 3: Upload Files to S3
For each file, app uploads directly to S3:

```http
PUT <upload_url>
Content-Type: image/jpeg

[Binary file data]
```

**Progress Tracking:**
- App tracks upload progress for each file
- Shows progress bars (0-100%)
- Can upload multiple files in parallel

**Response from S3:**
- Status: 200 OK
- Headers include `ETag` (file checksum)

**App State:** Show progress bars for each file

#### Step 4: Complete Uploads
For each uploaded file, app confirms completion:

```http
POST /api/v1/posts/upload/complete/{media_id}
Content-Type: application/json
Authorization: Bearer <access_token>

{
  "etag": "\"d41d8cd98f00b204e9800998ecf8427e\"",
  "size_bytes": 2048576
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "media_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "completed",
    "download_url": "https://s3.amazonaws.com/bucket/media-id/original.jpg"
  }
}
```

**App State:** Show "Processing files..." indicator

#### Step 5: Create Post
Once all files are uploaded and completed, app creates the post:

```http
POST /api/v1/posts
Content-Type: application/json
Authorization: Bearer <access_token>

{
  "post_type": "image",
  "mood": "adventure",
  "caption": "Amazing sunset!",
  "location": "Bali, Indonesia",
  "tags": ["travel", "beach", "sunset"],
  "media_items": [
    {
      "media_id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "image",
      "position": 0
    },
    {
      "media_id": "660e8400-e29b-41d4-a716-446655440001",
      "type": "image",
      "position": 1
    }
  ]
}
```

**App State:** Show "Publishing..." indicator

**Response:**
```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "post-123",
    "post_type": "image",
    "caption": "Amazing sunset!",
    "mood": "adventure",
    "media_items": [
      {
        "id": "1",
        "url": "https://s3.amazonaws.com/bucket/media-id-1/original.jpg",
        "type": "image",
        "position": 0
      }
    ]
  }
}
```

**App State:** Navigate to post or show success message

---

## 🔄 Complete Flow Diagram

### Multipart Flow
```
Mobile App                    Backend                    Media Service              S3
    │                            │                            │                      │
    │──1. POST /posts───────────>│                            │                      │
    │  (multipart/form-data)     │                            │                      │
    │                            │                            │                      │
    │                            │──2. Request upload URL───>│                      │
    │                            │                            │                      │
    │                            │<──3. Presigned URL─────────│                      │
    │                            │                            │                      │
    │                            │──4. PUT file───────────────┼─────────────────────>│
    │                            │                            │                      │
    │                            │<──5. ETag───────────────────┼──────────────────────│
    │                            │                            │                      │
    │                            │──6. Complete upload────────>│                      │
    │                            │                            │                      │
    │                            │<──7. Media ID──────────────│                      │
    │                            │                            │                      │
    │                            │──8. Create post───────────>│                      │
    │                            │  (with media IDs)          │                      │
    │                            │                            │                      │
    │                            │──9. Fetch URLs─────────────>│                      │
    │                            │                            │                      │
    │                            │<──10. S3 URLs──────────────│                      │
    │                            │                            │                      │
    │<──11. Post created─────────│                            │                      │
    │  (with S3 URLs)            │                            │                      │
```

### Presigned S3 Flow
```
Mobile App                    Backend                    Media Service              S3
    │                            │                            │                      │
    │──1. POST /upload-urls──────>│                            │                      │
    │                            │──2. Request presigned URL──>│                      │
    │                            │                            │                      │
    │<──3. Presigned URL─────────│<──3. Presigned URL─────────│                      │
    │                            │                            │                      │
    │──4. PUT file────────────────┼────────────────────────────┼─────────────────────>│
    │  (direct to S3)            │                            │                      │
    │                            │                            │                      │
    │<──5. ETag───────────────────┼────────────────────────────┼──────────────────────│
    │                            │                            │                      │
    │──6. POST /upload/complete──>│                            │                      │
    │  (with ETag)               │──7. Confirm upload─────────>│                      │
    │                            │                            │                      │
    │<──8. Media ID───────────────│<──8. Media ID──────────────│                      │
    │                            │                            │                      │
    │──9. POST /posts─────────────>│                            │                      │
    │  (with media IDs)          │──10. Create post───────────>│                      │
    │                            │                            │                      │
    │                            │──11. Fetch URLs────────────>│                      │
    │                            │                            │                      │
    │                            │<──12. S3 URLs───────────────│                      │
    │                            │                            │                      │
    │<──13. Post created──────────│                            │                      │
    │  (with S3 URLs)            │                            │                      │
```

---

## 📊 Comparison

| Feature | Multipart Flow | Presigned S3 Flow |
|---------|---------------|-------------------|
| **Complexity** | Simple (1 API call) | More complex (4+ API calls) |
| **Server Load** | High (files go through server) | Low (files go directly to S3) |
| **Upload Speed** | Slower (server bottleneck) | Faster (direct to S3) |
| **Progress Tracking** | Limited | Full control |
| **Error Handling** | Server handles | Client handles |
| **Best For** | Small files, simple apps | Large files, production apps |

---

## 🎯 Key Points

1. **Media IDs**: Both flows use media IDs from the media service, not direct URLs
2. **S3 URLs**: URLs are fetched from media service when retrieving posts (they're presigned and expire)
3. **Post Type**: Automatically determined from media items (image/video/mixed)
4. **Multiple Files**: Both flows support multiple files (carousel posts)
5. **Error Handling**: If any step fails, the entire flow should be retried or cancelled

---

## 🔐 Security

- All endpoints require authentication (Bearer token)
- Presigned URLs expire after 1 hour
- Files are validated on both client and server
- Media service manages S3 access

---

## 📱 Mobile App Implementation Tips

1. **Use multipart flow** for MVP or small files
2. **Use presigned flow** for production with large files
3. **Show progress indicators** at each step
4. **Handle errors gracefully** with retry options
5. **Validate files** before uploading
6. **Upload files in parallel** for better UX
7. **Cache media IDs** in case of network errors during post creation

