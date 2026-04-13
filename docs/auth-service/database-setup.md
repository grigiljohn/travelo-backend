# Auth Service - Database Setup

## Database Schema

The auth-service requires the following tables in the `travelo_auth` database:

1. **users** - User accounts
2. **otps** - OTP verification codes
3. **devices** - Device tracking
4. **password_reset_tokens** - Password reset tokens

---

## Quick Setup

### Option 1: Run Complete Schema Script

```bash
psql -U travelo -d travelo_auth -f scripts/create-all-auth-tables.sql
```

### Option 2: Run Individual Scripts

```bash
# Users and OTPs
psql -U travelo -d travelo_auth -f scripts/create-auth-tables-quick.sql

# Devices
psql -U travelo -d travelo_auth -f scripts/create-device-table.sql

# Password Reset Tokens
psql -U travelo -d travelo_auth -f scripts/create-password-reset-table.sql
```

---

## Table Descriptions

### users
Stores user account information.

**Columns:**
- `id` (UUID, Primary Key)
- `name` (VARCHAR(50))
- `username` (VARCHAR(30), Unique)
- `email` (VARCHAR(255), Unique)
- `password` (VARCHAR(255)) - BCrypt hashed
- `mobile` (VARCHAR(20))
- `is_email_verified` (BOOLEAN)
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)
- `last_login_at` (TIMESTAMP WITH TIME ZONE)

**Indexes:**
- `idx_username` on `username` (unique)
- `idx_email` on `email` (unique)

---

### otps
Stores one-time passwords for email verification.

**Columns:**
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key → users.id)
- `email` (VARCHAR(255))
- `otp` (VARCHAR(10))
- `expires_at` (TIMESTAMP WITH TIME ZONE)
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `is_used` (BOOLEAN)

**Indexes:**
- `idx_otp_email` on `email`
- `idx_otp_user_id` on `user_id`
- `idx_otp_email_otp` on `(email, otp)` where `is_used = FALSE`

---

### devices
Tracks user devices for authentication sessions.

**Columns:**
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key → users.id)
- `device_id` (VARCHAR(255)) - Device fingerprint
- `device_name` (VARCHAR(100))
- `device_type` (VARCHAR(50)) - MOBILE, TABLET, DESKTOP
- `user_agent` (VARCHAR(500))
- `ip_address` (VARCHAR(45))
- `is_trusted` (BOOLEAN)
- `last_used_at` (TIMESTAMP WITH TIME ZONE)
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

**Indexes:**
- `idx_device_user_id` on `user_id`
- `idx_device_device_id` on `device_id`
- `idx_user_device` on `(user_id, device_id)` (unique)

---

### password_reset_tokens
Stores password reset tokens.

**Columns:**
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key → users.id)
- `token` (VARCHAR(255), Unique) - Secure reset token
- `expires_at` (TIMESTAMP WITH TIME ZONE)
- `is_used` (BOOLEAN)
- `created_at` (TIMESTAMP WITH TIME ZONE)

**Indexes:**
- `idx_reset_token_user_id` on `user_id`
- `idx_reset_token_token` on `token` (unique)
- `idx_reset_token_expires_at` on `expires_at`

---

## Verification

After creating tables, verify they exist:

```sql
-- Connect to database
\c travelo_auth

-- List all tables
\dt

-- Should show:
-- users
-- otps
-- devices
-- password_reset_tokens

-- Check indexes
\di

-- Verify table structures
\d users
\d otps
\d devices
\d password_reset_tokens
```

---

## Maintenance

### Cleanup Expired Tokens

The service automatically cleans up expired password reset tokens daily at 2 AM. You can also manually clean:

```sql
-- Clean expired password reset tokens
DELETE FROM password_reset_tokens 
WHERE expires_at < NOW() OR is_used = TRUE;

-- Clean expired OTPs (optional)
DELETE FROM otps 
WHERE expires_at < NOW() AND is_used = TRUE;
```

### Backup

```bash
# Backup database
pg_dump -U travelo travelo_auth > backup_$(date +%Y%m%d).sql

# Restore from backup
psql -U travelo -d travelo_auth < backup_YYYYMMDD.sql
```

---

## Troubleshooting

### Tables Not Created

1. Verify database exists: `\l` (should see `travelo_auth`)
2. Check user permissions: `\du`
3. Verify connection: `\c travelo_auth`
4. Check for errors in script execution

### Permission Errors

```sql
-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE travelo_auth TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO travelo;
```

---

## Production Considerations

1. **Backups**: Set up automated daily backups
2. **Monitoring**: Monitor table sizes and growth
3. **Indexes**: Monitor index usage and optimize
4. **Connection Pool**: Configure HikariCP appropriately
5. **Maintenance Windows**: Schedule cleanup during low traffic

