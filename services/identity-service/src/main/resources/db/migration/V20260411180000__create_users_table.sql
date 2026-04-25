-- Base `users` table. Version is before V20260411194300 (ALTER) so a fresh
-- `travelo_auth` database creates the table before the ALTER.
-- CREATE IF NOT EXISTS is safe for DBs that already have `users` from older Hibernate/DDL.
CREATE TABLE IF NOT EXISTS users (
    id                uuid PRIMARY KEY,
    name              varchar(50)  NOT NULL,
    username          varchar(30)  NOT NULL,
    email             varchar(255) NOT NULL,
    password          varchar(255) NOT NULL,
    mobile            varchar(20),
    is_email_verified boolean      NOT NULL DEFAULT false,
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    last_login_at     timestamptz,
    profile_picture_url varchar(2048),
    cover_photo_url   varchar(2048),
    bio                 varchar(500),
    is_private        boolean      NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_email ON users (email);
