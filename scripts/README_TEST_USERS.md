# Test Users Scripts

This directory contains SQL scripts to insert test users into the `travelo_auth` database.

## Files

1. **`insert_test_users.sql`** - Full script with 10 diverse test users
2. **`insert_test_users_simple.sql`** - Simple script with 5 basic test users

## Database Information

- **Database**: `travelo_auth`
- **Table**: `users`
- **Default Connection**: `localhost:5432`

## Usage

### Option 1: Using psql command line

```bash
# Connect to database
psql -U travelo -d travelo_auth

# Run the script
\i scripts/insert_test_users.sql
```

### Option 2: Using psql with file input

```bash
psql -U travelo -d travelo_auth -f scripts/insert_test_users.sql
```

### Option 3: Copy-paste directly

Open the SQL file and copy-paste the INSERT statements into your PostgreSQL client (pgAdmin, DBeaver, etc.)

## Test User Credentials

**All users have the same password**: `password123`

### Full Script Users (10 users):

| Username | Email | Email Verified |
|----------|-------|----------------|
| alice_travels | alice@test.travelo.com | ✅ |
| bob_explorer | bob@test.travelo.com | ✅ |
| charlie_eats | charlie@test.travelo.com | ✅ |
| diana_shots | diana@test.travelo.com | ❌ |
| eve_city | eve@test.travelo.com | ✅ |
| frank_sands | frank@test.travelo.com | ✅ |
| grace_mountain | grace@test.travelo.com | ❌ |
| henry_arts | henry@test.travelo.com | ✅ |
| ivy_nights | ivy@test.travelo.com | ✅ |
| jack_wanderer | jack@test.travelo.com | ✅ |

### Simple Script Users (5 users):

| Username | Email |
|----------|-------|
| testuser1 | testuser1@test.travelo.com |
| testuser2 | testuser2@test.travelo.com |
| testuser3 | testuser3@test.travelo.com |
| testuser4 | testuser4@test.travelo.com |
| testuser5 | testuser5@test.travelo.com |

## Password Hash

The password `password123` is hashed using BCrypt with 10 rounds:
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

## Verification

After running the script, verify the users were inserted:

```sql
SELECT 
    id, 
    name, 
    username, 
    email, 
    is_email_verified,
    created_at
FROM users 
WHERE email LIKE '%@test.travelo.com'
ORDER BY created_at DESC;
```

## Troubleshooting

### Error: function gen_random_uuid() does not exist

If you're using PostgreSQL < 13, use `uuid_generate_v4()` instead:

1. Enable the extension:
   ```sql
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   ```

2. Replace `gen_random_uuid()` with `uuid_generate_v4()` in the script

### Error: duplicate key value violates unique constraint

The script uses `ON CONFLICT (email) DO NOTHING` to skip existing users. If you want to delete and re-insert:

```sql
DELETE FROM users WHERE email LIKE '%@test.travelo.com';
```

Then run the insert script again.

### Error: relation "users" does not exist

Make sure you're connected to the correct database (`travelo_auth`) and that the users table exists. The table should be created automatically by Hibernate when the auth-service starts.

## Notes

- All users are created with current timestamp for `created_at` and `updated_at`
- Some users have `is_email_verified = false` to test unverified user flows
- Mobile numbers are placeholder values
- The scripts use `ON CONFLICT DO NOTHING` to prevent errors if users already exist

