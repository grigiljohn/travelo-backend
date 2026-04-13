package com.travelo.postservice.dto;

import com.travelo.postservice.entity.MusicSource;
import com.travelo.postservice.entity.PostMedia;

import java.util.List;
import java.util.UUID;

public record PostRequest(
        String caption,
        UUID authorId,
        MusicPayload music,
        List<MediaPayload> media
) {
    public record MusicPayload(
            String title,
            String artist,
            String album,
            Integer duration,
            String genre,
            String audioUrl,
            String coverImageUrl,
            MusicSource source
    ) {
    }

    public record MediaPayload(
            String mediaUrl,
            String thumbnailUrl,
            PostMedia.MediaType mediaType,
            Integer orderIndex
    ) {
    }
}

