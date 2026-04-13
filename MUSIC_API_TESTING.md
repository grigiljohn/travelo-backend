# Music API Testing Guide for Postman

## Base URLs

### Via API Gateway (Recommended - matches frontend)
- **Base URL**: `http://localhost:8080`
- **Music Service Path**: `/music-service`
- **Full Base**: `http://localhost:8080/music-service`

### Direct to Music Service (Bypass Gateway)
- **Direct URL**: `http://localhost:8089`
- **Note**: Music service runs on port 8089

---

## API Endpoints

### 1. Get Recommended Music Tracks
**Endpoint**: `GET /api/v1/music/recommended`

**Via Gateway**:
```
GET http://localhost:8080/music-service/api/v1/music/recommended
```

**Direct**:
```
GET http://localhost:8089/api/v1/music/recommended
```

**Headers**:
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Expected Response** (200 OK):
```json
[
  {
    "id": "uuid-here",
    "name": "Track Name",
    "artist": "Artist Name",
    "mood": "chill",
    "durationSeconds": 180,
    "thumbnailUrl": "https://...",
    "fileUrl": "https://...",
    "isRecommended": true
  }
]
```

---

### 2. Get Music Tracks by Mood
**Endpoint**: `GET /api/v1/music/mood/{mood}`

**Via Gateway**:
```
GET http://localhost:8080/music-service/api/v1/music/mood/chill
GET http://localhost:8080/music-service/api/v1/music/mood/romantic
GET http://localhost:8080/music-service/api/v1/music/mood/energetic
GET http://localhost:8080/music-service/api/v1/music/mood/calm
GET http://localhost:8080/music-service/api/v1/music/mood/happy
```

**Direct**:
```
GET http://localhost:8089/api/v1/music/mood/chill
```

**Headers**:
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Path Parameters**:
- `mood` (required): One of: `chill`, `romantic`, `energetic`, `calm`, `happy`, etc.

**Expected Response** (200 OK):
```json
[
  {
    "id": "uuid-here",
    "name": "Track Name",
    "artist": "Artist Name",
    "mood": "chill",
    "durationSeconds": 180,
    "thumbnailUrl": "https://...",
    "fileUrl": "https://...",
    "isRecommended": false
  }
]
```

---

### 3. Search Music Tracks
**Endpoint**: `GET /api/v1/music/search?q={query}`

**Via Gateway**:
```
GET http://localhost:8080/music-service/api/v1/music/search?q=summer
```

**Direct**:
```
GET http://localhost:8089/api/v1/music/search?q=summer
```

**Headers**:
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Query Parameters**:
- `q` (optional, defaults to empty string): Search query

**Expected Response** (200 OK):
```json
[
  {
    "id": "uuid-here",
    "name": "Summer Vibes",
    "artist": "Artist Name",
    "mood": "happy",
    "durationSeconds": 180,
    "thumbnailUrl": "https://...",
    "fileUrl": "https://...",
    "isRecommended": false
  }
]
```

---

### 4. Get All Music Tracks
**Endpoint**: `GET /api/v1/music`

**Via Gateway**:
```
GET http://localhost:8080/music-service/api/v1/music
```

**Direct**:
```
GET http://localhost:8089/api/v1/music
```

**Headers**:
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Expected Response** (200 OK):
```json
[
  {
    "id": "uuid-here",
    "name": "Track Name",
    "artist": "Artist Name",
    "mood": "chill",
    "durationSeconds": 180,
    "thumbnailUrl": "https://...",
    "fileUrl": "https://...",
    "isRecommended": true
  }
]
```

---

## Testing Steps

### Step 1: Test Direct Service (Bypass Gateway)
1. Ensure music-service is running on port 8089
2. Test direct endpoint:
   ```
   GET http://localhost:8089/api/v1/music/recommended
   ```
3. **No authentication required** (check if controller requires auth)

### Step 2: Test Via API Gateway
1. Ensure API Gateway is running on port 8080
2. Ensure music-service is running and registered (or use direct URL in gateway config)
3. Test gateway endpoint:
   ```
   GET http://localhost:8080/music-service/api/v1/music/recommended
   ```
4. Add Authorization header if required

### Step 3: Check Database
1. Connect to PostgreSQL:
   ```bash
   psql -U travelo -d travelo_music
   ```
2. Check if table exists:
   ```sql
   \dt music_tracks
   ```
3. Check if data exists:
   ```sql
   SELECT COUNT(*) FROM music_tracks;
   SELECT * FROM music_tracks LIMIT 5;
   ```

### Step 4: Check Service Logs
- Check music-service logs for errors
- Check API Gateway logs for routing issues
- Look for 404, 503, or 500 errors

---

## Common Issues & Solutions

### Issue 1: 404 Not Found
**Possible Causes**:
- API Gateway route not configured correctly
- Service not running
- Wrong URL path

**Solution**:
- Check API Gateway config: `services/api-gateway/src/main/resources/application.yml`
- Verify music-service route: `/music-service/**` → `http://localhost:8089`
- Test direct service first to isolate gateway issues

### Issue 2: 503 Service Unavailable
**Possible Causes**:
- Music-service not running
- Eureka not running (if using `lb://music-service`)
- Service not registered with Eureka

**Solution**:
- Start music-service on port 8089
- Use direct URL in gateway config: `uri: http://localhost:8089`
- Check service health: `http://localhost:8089/actuator/health`

### Issue 3: Empty Array Response `[]`
**Possible Causes**:
- Database table is empty
- No data inserted
- Migration not run

**Solution**:
- Check database: `SELECT * FROM music_tracks;`
- Run migration: `V1__create_music_tracks_table.sql`
- Insert test data manually

### Issue 4: 401 Unauthorized
**Possible Causes**:
- JWT token missing or invalid
- Token expired

**Solution**:
- Get valid JWT token from login endpoint
- Add to headers: `Authorization: Bearer <token>`
- Check if endpoint requires authentication (may not be required)

---

## Database Setup

### Check if Table Exists
```sql
\c travelo_music
\dt music_tracks
```

### Insert Test Data
```sql
INSERT INTO music_tracks (id, name, artist, mood, duration_seconds, is_recommended, is_active, created_at, updated_at)
VALUES 
  (gen_random_uuid(), 'Chill Vibes', 'Artist 1', 'chill', 180, true, true, NOW(), NOW()),
  (gen_random_uuid(), 'Romantic Song', 'Artist 2', 'romantic', 200, true, true, NOW(), NOW()),
  (gen_random_uuid(), 'Energetic Beat', 'Artist 3', 'energetic', 150, false, true, NOW(), NOW());
```

### Verify Data
```sql
SELECT id, name, artist, mood, is_recommended, is_active 
FROM music_tracks 
ORDER BY created_at DESC;
```

---

## Frontend API Calls

The frontend makes these calls:
- **Recommended**: `http://10.0.2.2:8080/music-service/api/v1/music/recommended` (Android emulator)
- **By Mood**: `http://10.0.2.2:8080/music-service/api/v1/music/mood/{mood}`
- **Search**: `http://10.0.2.2:8080/music-service/api/v1/music/search?q={query}`

**Note**: Android emulator uses `10.0.2.2` instead of `localhost`

---

## Quick Test Commands

### Using cURL

**Recommended (via gateway)**:
```bash
curl -X GET "http://localhost:8080/music-service/api/v1/music/recommended" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Direct service**:
```bash
curl -X GET "http://localhost:8089/api/v1/music/recommended" \
  -H "Content-Type: application/json"
```

**By Mood**:
```bash
curl -X GET "http://localhost:8080/music-service/api/v1/music/mood/chill" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Search**:
```bash
curl -X GET "http://localhost:8080/music-service/api/v1/music/search?q=summer" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## Expected Response Format

All endpoints return an array of `MusicTrackResponse`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Track Name",
  "artist": "Artist Name",
  "mood": "chill",
  "durationSeconds": 180,
  "thumbnailUrl": "https://example.com/thumb.jpg",
  "fileUrl": "https://example.com/track.mp3",
  "isRecommended": true
}
```

---

## Service Configuration

- **Music Service Port**: 8089
- **API Gateway Port**: 8080
- **Database**: `travelo_music`
- **Table**: `music_tracks`
- **Migration**: `V1__create_music_tracks_table.sql`

