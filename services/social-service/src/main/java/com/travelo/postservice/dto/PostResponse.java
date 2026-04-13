package com.travelo.postservice.dto;

import com.travelo.postservice.entity.Music;
import com.travelo.postservice.entity.MusicSource;
import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.PostMedia;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        Long id,
        String caption,
        UUID authorId,
        OffsetDateTime createdAt,
        MusicResponse music,
        List<MediaResponse> media
) {

    public static PostResponse fromEntity(Post post) {
        MusicResponse musicResponse = null;
        if (post.getMusic() != null) {
            musicResponse = MusicResponse.fromEntity(post.getMusic());
        }
        List<MediaResponse> mediaResponses = post.getMedia()
                .stream()
                .map(MediaResponse::fromEntity)
                .toList();

        return new PostResponse(
                post.getId() != null ? Long.parseLong(post.getId()) : null, // Convert String id to Long for backward compatibility
                post.getCaption(),
                post.getUserId() != null ? UUID.fromString(post.getUserId()) : null, // Convert String userId to UUID
                post.getCreatedAt(),
                musicResponse,
                mediaResponses
        );
    }

    public record MusicResponse(
            Long id,
            String title,
            String artist,
            String album,
            Integer duration,
            String genre,
            String audioUrl,
            String coverImageUrl,
            MusicSource source,
            OffsetDateTime createdAt
    ) {
        public static MusicResponse fromEntity(Music music) {
            return new MusicResponse(
                    music.getId(),
                    music.getTitle(),
                    music.getArtist(),
                    music.getAlbum(),
                    music.getDuration(),
                    music.getGenre(),
                    music.getAudioUrl(),
                    music.getCoverImageUrl(),
                    music.getSource(),
                    music.getCreatedAt()
            );
        }
    }

    public record MediaResponse(
            Long id,
            String mediaUrl,
            String thumbnailUrl,
            PostMedia.MediaType mediaType,
            Integer orderIndex,
            OffsetDateTime createdAt
    ) {
        public static MediaResponse fromEntity(PostMedia media) {
            return new MediaResponse(
                    media.getId(),
                    media.getMediaUrl(),
                    media.getThumbnailUrl(),
                    media.getMediaType(),
                    media.getOrderIndex(),
                    media.getCreatedAt()
            );
        }
    }
}


