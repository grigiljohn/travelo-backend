package com.travelo.mediaservice.reel;

/**
 * A royalty-free style track referenced by URL (S3/CDN/local nginx). Replace URLs in config for production.
 */
public record ReelMusicTrack(String id, String title, ReelMusicCategory category, String url) {}
