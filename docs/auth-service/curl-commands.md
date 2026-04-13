# Auth Service - cURL Commands Quick Reference

## Base URL
- Direct: `http://localhost:8081`
- Gateway: `http://localhost:8080/auth-service`

---

## 🔓 Public Endpoints

### Register
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

### Login
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrUsername": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

### Refresh Token
```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### Forgot Password
```bash
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com"
  }'
```

### Reset Password
```bash
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "newPassword": "NewSecurePass123!"
  }'
```

---

## 🔒 Authenticated Endpoints

### Send Verification OTP
```bash
curl -X POST http://localhost:8081/api/v1/auth/send-verification-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{"email": "john.doe@example.com"}'
```

### Verify OTP
```bash
curl -X POST http://localhost:8081/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "john.doe@example.com",
    "otp": "123456"
  }'
```

### Resend OTP
```bash
curl -X POST http://localhost:8081/api/v1/auth/resend-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{"email": "john.doe@example.com"}'
```

### Logout
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Change Password
```bash
curl -X POST http://localhost:8081/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "currentPassword": "OldPass123!",
    "newPassword": "NewSecurePass123!"
  }'
```

### Get Devices
```bash
curl -X GET http://localhost:8081/api/v1/auth/devices \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Trust Device
```bash
curl -X POST http://localhost:8081/api/v1/auth/devices/device-id/trust \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Remove Device
```bash
curl -X DELETE http://localhost:8081/api/v1/auth/devices/device-id \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Deactivate Account
```bash
curl -X POST http://localhost:8081/api/v1/auth/deactivate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Delete Account
```bash
curl -X DELETE http://localhost:8081/api/v1/auth/account \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 🔄 Complete Flow Examples

### Registration → Email Verification → Login

```bash
# 1. Register
RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "mobile": "+1234567890"
  }')

# Extract token
ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.data.accessToken')
echo "Access Token: $ACCESS_TOKEN"

# 2. Check email for OTP or check logs/Redis
read -p "Enter OTP from email: " OTP

# 3. Verify OTP
curl -X POST http://localhost:8081/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"email\": \"john.doe@example.com\",
    \"otp\": \"$OTP\"
  }"

# 4. Login (after verification)
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrUsername": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

### Password Reset Flow

```bash
# 1. Request password reset
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "john.doe@example.com"}'

# 2. Check email for reset link/token
read -p "Enter reset token from email: " RESET_TOKEN

# 3. Reset password
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$RESET_TOKEN\",
    \"newPassword\": \"NewSecurePass123!\"
  }"
```

---

## 💻 PowerShell Examples

### Register and Login
```powershell
# Register
$body = @{
    name = "John Doe"
    username = "johndoe"
    email = "john.doe@example.com"
    password = "SecurePass123!"
    mobile = "+1234567890"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/register" `
    -Method Post -ContentType "application/json" -Body $body

$accessToken = $response.data.accessToken
Write-Host "Access Token: $accessToken"

# Login
$loginBody = @{
    emailOrUsername = "john.doe@example.com"
    password = "SecurePass123!"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" `
    -Method Post -ContentType "application/json" -Body $loginBody

$newAccessToken = $loginResponse.data.accessToken
Write-Host "New Access Token: $newAccessToken"
```

### Change Password
```powershell
$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

$changePassBody = @{
    currentPassword = "OldPass123!"
    newPassword = "NewSecurePass123!"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/change-password" `
    -Method Post -Headers $headers -Body $changePassBody
```

---

## 🧪 Quick Test Commands

### Test Registration
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","username":"testuser","email":"test@example.com","password":"Test123!@#"}' \
  | jq '.'
```

### Test Login
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailOrUsername":"test@example.com","password":"Test123!@#"}' \
  | jq '.'
```

### Test with Pretty Output
Add `| jq '.'` to any curl command for formatted JSON output.

---

## 📝 Notes

- Replace `YOUR_ACCESS_TOKEN` with actual token from login/register response
- All timestamps are in ISO 8601 format with timezone
- Error responses follow consistent format with `success: false`
- Rate limits reset automatically after the time window
- Account lockout clears after 15 minutes

