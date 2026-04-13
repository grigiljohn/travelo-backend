# cURL Commands for Post Service API

## Media Upload Endpoint

### Upload Image

**Basic command:**
```bash
curl -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@/path/to/your/image.jpg" \
  -F "media_type=image"
```

**With verbose output:**
```bash
curl -v -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@/path/to/your/image.jpg" \
  -F "media_type=image"
```

**Pretty JSON output:**
```bash
curl -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@/path/to/your/image.jpg" \
  -F "media_type=image" | jq
```

### Upload Video

**Basic command:**
```bash
curl -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@/path/to/your/video.mp4" \
  -F "media_type=video"
```

**With verbose output:**
```bash
curl -v -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@/path/to/your/video.mp4" \
  -F "media_type=video"
```

### Supported File Types

**Images:**
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)

**Videos:**
- MP4 (.mp4)
- QuickTime (.mov)
- AVI (.avi)
- WebM (.webm)

### File Size Limits

- **Images**: Maximum 50MB
- **Videos**: Maximum 500MB

### Example Responses

**Success Response:**
```json
{
  "success": true,
  "message": "Media uploaded successfully",
  "data": {
    "url": "http://localhost:8083/uploads/uuid-filename.jpg",
    "media_id": "uuid-here",
    "media_type": "image",
    "file_size": 1024000,
    "width": null,
    "height": null,
    "duration": null,
    "thumbnail_url": null,
    "uploaded_at": "2025-11-20T17:30:00Z"
  },
  "timestamp": "2025-11-20T17:30:00Z"
}
```

**Error Response (File too large):**
```json
{
  "success": false,
  "message": "File size exceeds maximum allowed size of 50MB",
  "error_code": "FILE_TOO_LARGE",
  "timestamp": "2025-11-20T17:30:00Z"
}
```

**Error Response (Invalid file type):**
```json
{
  "success": false,
  "message": "Invalid file type. Only images and videos are allowed.",
  "error_code": "INVALID_FILE_TYPE",
  "timestamp": "2025-11-20T17:30:00Z"
}
```

## Windows PowerShell Examples

**Upload Image (PowerShell):**
```powershell
$filePath = "C:\path\to\your\image.jpg"

curl.exe -X POST http://localhost:8083/api/v1/media/upload `
  -F "file=@$filePath" `
  -F "media_type=image"
```

**Upload Video (PowerShell):**
```powershell
$filePath = "C:\path\to\your\video.mp4"

curl.exe -X POST http://localhost:8083/api/v1/media/upload `
  -F "file=@$filePath" `
  -F "media_type=video"
```

## Using the Uploaded Media URL

After uploading, you'll receive a URL in the response. Use this URL in the `media_items` array when creating a post:

```bash
curl -X POST http://localhost:8083/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{
    "post_type": "image",
    "media_items": [
      {
        "url": "http://localhost:8083/uploads/uuid-filename.jpg",
        "type": "image",
        "position": 0
      }
    ],
    "caption": "My awesome photo!",
    "tags": ["travel", "adventure"],
    "mood": "adventure"
  }'
```

## Quick Test Commands

**Test with a sample image (if you have one):**
```bash
# Create a test image (requires ImageMagick or similar)
# Or use an existing image file

curl -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@test-image.jpg" \
  -F "media_type=image"
```

**Test error handling (empty file):**
```bash
# This should return an error
curl -X POST http://localhost:8083/api/v1/media/upload \
  -F "file=@empty.txt" \
  -F "media_type=image"
```

## Notes

1. Authentication is not implemented yet - all operations use a default system user
2. The file path must be absolute or relative to your current directory
3. Use `-F` flag for multipart/form-data (required for file uploads)
4. The `@` symbol before the file path tells curl to read the file
5. Make sure the service is running on port 8083
6. Uploaded files are stored in the `uploads` directory (relative to the application)

