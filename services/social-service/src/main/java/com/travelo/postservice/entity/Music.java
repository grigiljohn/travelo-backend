package com.travelo.postservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "music")
public class Music {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String artist;

    @Column(length = 255)
    private String album;

    private Integer duration;

    @Column(length = 100)
    private String genre;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MusicSource source = MusicSource.LOCAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Music() {
        // for JPA
    }

    public Music(String title, String artist, String album, Integer duration, String genre,
                 String audioUrl, String coverImageUrl, MusicSource source) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.genre = genre;
        this.audioUrl = audioUrl;
        this.coverImageUrl = coverImageUrl;
        if (source != null) {
            this.source = source;
        }
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public MusicSource getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", genre='" + genre + '\'' +
                ", audioUrl='" + audioUrl + '\'' +
                ", coverImageUrl='" + coverImageUrl + '\'' +
                ", source=" + source +
                ", createdAt=" + createdAt +
                '}';
    }
}


