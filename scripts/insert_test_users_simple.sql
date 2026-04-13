-- ============================================
-- Simple Test Users Insert Script (PostgreSQL 13+)
-- Database: travelo_auth
-- Table: users
-- ============================================
-- 
-- Quick script to insert 5 essential test users
-- Password for all: "password123"
-- 
-- ============================================

INSERT INTO users (id, name, username, email, password, mobile, is_email_verified, created_at, updated_at) VALUES
(gen_random_uuid(), 'Test User 1', 'testuser1', 'testuser1@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567890', true, NOW(), NOW()),
(gen_random_uuid(), 'Test User 2', 'testuser2', 'testuser2@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567891', true, NOW(), NOW()),
(gen_random_uuid(), 'Test User 3', 'testuser3', 'testuser3@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567892', true, NOW(), NOW()),
(gen_random_uuid(), 'Test User 4', 'testuser4', 'testuser4@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567893', true, NOW(), NOW()),
(gen_random_uuid(), 'Test User 5', 'testuser5', 'testuser5@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567894', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- View inserted users
SELECT username, email, is_email_verified FROM users WHERE email LIKE '%@test.travelo.com';

