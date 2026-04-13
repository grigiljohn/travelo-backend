-- Keep PostgreSQL `users` in sync with com.travelo.authservice.entity.User
-- (Hibernate ddl-auto:update sometimes skips NOT NULL columns on existing DBs.)

ALTER TABLE users ADD COLUMN IF NOT EXISTS is_private boolean NOT NULL DEFAULT false;

ALTER TABLE users ADD COLUMN IF NOT EXISTS bio varchar(500);

ALTER TABLE users ADD COLUMN IF NOT EXISTS cover_photo_url varchar(2048);

ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture_url varchar(2048);
