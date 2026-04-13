# Media Service

Handles media uploads (local storage), processing, thumbnails, and virus scanning.

## Local storage configuration

All files are stored on local disk. No S3 or cloud storage required.

| Variable | Description | Default |
|----------|-------------|---------|
| `MEDIA_STORAGE_PATH` | Base directory for uploads | `./uploads` |
| `MEDIA_STORAGE_BASE_URL` | Base URL for serving files (e.g. for download links) | `http://localhost:8084` |

## Direct upload

POST `/v1/media/upload` (multipart/form-data):
- `owner_id` (UUID)
- `file` (file)
- `filename` (optional)
- `media_type` (optional, default: image)

Returns `{ mediaId, downloadUrl, storageKey }`.
