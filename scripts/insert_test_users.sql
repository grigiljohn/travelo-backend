-- ============================================
-- Test Users Insert Script for Travelo
-- Database: travelo_auth
-- Table: users
-- ============================================
-- 
-- This script creates 10 test users for development and testing.
-- All users have the password: "password123" (BCrypt hashed)
-- 
-- Usage:
--   1. Connect to PostgreSQL: psql -U travelo -d travelo_auth
--   2. Run this script: \i insert_test_users.sql
--   3. Or copy-paste the INSERT statements directly
--
-- ============================================

-- Note: gen_random_uuid() is built-in to PostgreSQL 13+
-- For older versions, use: uuid_generate_v4() after enabling uuid-ossp extension
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Clear existing test users (optional - uncomment if you want to reset)
-- DELETE FROM users WHERE email LIKE '%@test.travelo.com';

-- Insert Test Users
-- Password for all users: "password123"
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO users (id, name, username, email, password, mobile, is_email_verified, created_at, updated_at) VALUES
-- User 1: Travel Enthusiast
(gen_random_uuid(), 'Alice Johnson', 'alice_travels', 'alice@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567890', true, NOW(), NOW()),

-- User 2: Adventure Seeker
(gen_random_uuid(), 'Bob Adventure', 'bob_explorer', 'bob@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567891', true, NOW(), NOW()),

-- User 3: Food Blogger
(gen_random_uuid(), 'Charlie Foodie', 'charlie_eats', 'charlie@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567892', true, NOW(), NOW()),

-- User 4: Nature Photographer
(gen_random_uuid(), 'Diana Nature', 'diana_shots', 'diana@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567893', false, NOW(), NOW()),

-- User 5: City Explorer
(gen_random_uuid(), 'Eve Urban', 'eve_city', 'eve@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567894', true, NOW(), NOW()),

-- User 6: Beach Lover
(gen_random_uuid(), 'Frank Beach', 'frank_sands', 'frank@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567895', true, NOW(), NOW()),

-- User 7: Mountain Climber
(gen_random_uuid(), 'Grace Peaks', 'grace_mountain', 'grace@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567896', false, NOW(), NOW()),

-- User 8: Culture Explorer
(gen_random_uuid(), 'Henry Culture', 'henry_arts', 'henry@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567897', true, NOW(), NOW()),

-- User 9: Nightlife Enthusiast
(gen_random_uuid(), 'Ivy Nightlife', 'ivy_nights', 'ivy@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567898', true, NOW(), NOW()),

-- User 10: Solo Traveler
(gen_random_uuid(), 'Jack Solo', 'jack_wanderer', 'jack@test.travelo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+1234567899', true, NOW(), NOW())

ON CONFLICT (email) DO NOTHING; -- Skip if user already exists

-- Verify the inserted users
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

-- ============================================
-- Test User Credentials Summary
-- ============================================
-- All users have the same password: password123
--
-- 1. alice_travels / alice@test.travelo.com
-- 2. bob_explorer / bob@test.travelo.com
-- 3. charlie_eats / charlie@test.travelo.com
-- 4. diana_shots / diana@test.travelo.com (email not verified)
-- 5. eve_city / eve@test.travelo.com
-- 6. frank_sands / frank@test.travelo.com
-- 7. grace_mountain / grace@test.travelo.com (email not verified)
-- 8. henry_arts / henry@test.travelo.com
-- 9. ivy_nights / ivy@test.travelo.com
-- 10. jack_wanderer / jack@test.travelo.com
--
-- ============================================

