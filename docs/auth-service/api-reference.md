# Auth Service - Complete API Reference

## Base URLs

- **Direct Service**: `http://localhost:8081`
- **Via API Gateway**: `http://localhost:8080/auth-service`

---

## 🔓 Public Endpoints (No Authentication Required)

### 1. Register User

**POST** `/api/v1/auth/register`

Creates a new user account, generates JWT tokens, and sends OTP for email verification.

**Request:**
```json
{
  "name": "John Doe",
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "mobile": "+1234567890"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "mobile": "+1234567890"
  }'
```

**Response (201):**
```json
{
  "success": true,
  "message": "Account created successfully. Please verify your email.",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "username": "johndoe",
    "name": "John Doe",
    "isEmailVerified": false,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

---

### 2. Login

**POST** `/api/v1/auth/login`

Authenticates user with email/username and password. Includes account lockout protection and rate limiting.

**Request:**
```json
{
  "emailOrUsername": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrUsername": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

**Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "username": "johndoe",
    "name": "John Doe",
    "isEmailVerified": true,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

**Security Features:**
- Account lockout after 5 failed attempts (15 min lockout)
- Rate limiting: 10 requests/minute per IP
- Device tracking
- Remaining attempts counter

---

### 3. Refresh Token

**POST** `/api/v1/auth/refresh-token`

Generates new access and refresh tokens using a valid refresh token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

---

### 4. Forgot Password

**POST** `/api/v1/auth/forgot-password`

Sends a password reset link to the user's email. Always returns success to prevent email enumeration.

**Request:**
```json
{
  "email": "john.doe@example.com"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

**Response (200):**
```json
{
  "success": true,
  "message": "If an account exists with this email, a password reset link has been sent.",
  "data": null
}
```

---

### 5. Reset Password

**POST** `/api/v1/auth/reset-password`

Resets password using a valid reset token from email.

**Request:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePass123!"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "newPassword": "NewSecurePass123!"
  }'
```

---

## 🔒 Authenticated Endpoints (Bearer Token Required)

### 6. Send Verification OTP

**POST** `/api/v1/auth/send-verification-otp`

Sends a new OTP code to the user's email for verification.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/send-verification-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

---

### 7. Verify OTP

**POST** `/api/v1/auth/verify-otp`

Verifies the OTP code and marks email as verified.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "john.doe@example.com",
    "otp": "123456"
  }'
```

---

### 8. Resend OTP

**POST** `/api/v1/auth/resend-otp`

Resends OTP code (subject to 60-second cooldown).

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/resend-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

---

### 9. Logout

**POST** `/api/v1/auth/logout`

Logs out user and blacklists the token server-side.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 10. Change Password

**POST** `/api/v1/auth/change-password`

Changes password for authenticated user. Requires current password and **verified email**.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "currentPassword": "OldPass123!",
    "newPassword": "NewSecurePass123!"
  }'
```

**Features:**
- **Requires verified email** (returns 403 if not verified)
- Invalidates all existing tokens (forces re-login)
- Prevents reusing current password

**Error Response (403):**
```json
{
  "success": false,
  "message": "Email verification required. Please verify your email address before performing this action.",
  "errorCode": "EMAIL_NOT_VERIFIED"
}
```

---

### 11. Get User Devices

**GET** `/api/v1/auth/devices`

Returns list of all devices associated with the authenticated user.

**cURL:**
```bash
curl -X GET http://localhost:8081/api/v1/auth/devices \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "device-uuid",
      "deviceId": "fingerprint-hash",
      "deviceName": "Chrome on Windows",
      "deviceType": "DESKTOP",
      "isTrusted": true,
      "ipAddress": "192.168.1.1",
      "lastUsedAt": "2025-12-05T20:30:00Z",
      "createdAt": "2025-12-01T10:00:00Z"
    }
  ]
}
```

---

### 12. Trust Device

**POST** `/api/v1/auth/devices/{deviceId}/trust`

Marks a device as trusted.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/devices/device-fingerprint-hash/trust \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 13. Remove Device

**DELETE** `/api/v1/auth/devices/{deviceId}`

Removes a device from user's device list.

**cURL:**
```bash
curl -X DELETE http://localhost:8081/api/v1/auth/devices/device-fingerprint-hash \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 14. Deactivate Account

**POST** `/api/v1/auth/deactivate`

Deactivates the authenticated user's account. **Requires verified email**.

**cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/deactivate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Note:** Returns 403 if email is not verified.

---

### 15. Delete Account

**DELETE** `/api/v1/auth/account`

Permanently deletes the authenticated user's account (GDPR compliance). **Requires verified email**.

**cURL:**
```bash
curl -X DELETE http://localhost:8081/api/v1/auth/account \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Note:** Returns 403 if email is not verified.

---

## 🔐 Security Features

### Account Protection
- Account lockout after 5 failed login attempts (15 min lockout)
- Rate limiting: 10 login attempts/minute per IP
- Failed attempt tracking in Redis
- Remaining attempts counter

### Token Security
- Token blacklisting on logout (Redis)
- Token invalidation on password change
- Token expiration: 1 hour (access), 7 days (refresh)
- Secure token generation

### Password Security
- BCrypt hashing (strength 12)
- Password strength validation
- Secure password reset tokens
- Password change verification

### Device Security
- Device fingerprinting (SHA-256 hash)
- New device detection
- Device trust management
- IP address tracking

---

## 📊 Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| Login | 10 requests | 1 minute (per IP) |
| OTP Generation | 3 requests | 10 minutes (per email) |
| OTP Verification | 5 attempts | 30 seconds (per email) |
| Password Reset | 3 requests | 1 hour (per email) |

---

## 🚨 Error Codes

- `INVALID_CREDENTIALS` - Wrong email/username or password
- `ACCOUNT_LOCKED` - Account temporarily locked
- `RATE_LIMIT_EXCEEDED` - Too many requests
- `INVALID_TOKEN` - Invalid or expired token
- `TOKEN_EXPIRED` - Token has expired
- `INVALID_OTP` - Invalid or expired OTP
- `EMAIL_ALREADY_VERIFIED` - Email is already verified
- `EMAIL_NOT_VERIFIED` - Email verification required for this operation
- `UNAUTHORIZED` - Missing or invalid authentication
- `FORBIDDEN` - Operation not allowed (e.g., email not verified)

---

## 📝 Validation Rules

### Password
- Minimum 8 characters, maximum 128
- Must contain: uppercase, lowercase, digit, special character

### Email
- Valid email format
- Pattern: `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`

### Username
- 3-30 characters
- Alphanumeric and underscores only

### Name
- 2-50 characters
- Letters, spaces, hyphens, apostrophes only

---

## 🔗 Swagger UI

Access interactive API documentation at:
- `http://localhost:8081/swagger-ui.html`

