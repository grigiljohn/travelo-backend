package com.travelo.mediaservice.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a processed variant of a media file.
 * Stored as JSON in the media.variants JSONB array.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaVariant {
    private String name;
    private String key;
    private String mime;
    private Integer width;
    private Integer height;
    private Integer bitrate;
    private Double duration;

    public MediaVariant() {
    }

    public MediaVariant(String name, String key, String mime) {
        this.name = name;
        this.key = key;
        this.mime = mime;
    }

    public MediaVariant(String name, String key, String mime, Integer width, Integer height) {
        this.name = name;
        this.key = key;
        this.mime = mime;
        this.width = width;
        this.height = height;
    }

    public MediaVariant(String name, String key, String mime, Integer width, Integer height, Integer bitrate, Double duration) {
        this.name = name;
        this.key = key;
        this.mime = mime;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "MediaVariant{" +
                "name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", mime='" + mime + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", bitrate=" + bitrate +
                ", duration=" + duration +
                '}';
    }
}

