package com.travelo.searchservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;

/**
 * Elasticsearch document for hashtags.
 */
@Document(indexName = "hashtags")
public class HashtagDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String tag;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name; // Display name (without #)

    @Field(type = FieldType.Integer)
    private Integer postCount;

    @Field(type = FieldType.Date)
    private OffsetDateTime createdAt;

    @Field(type = FieldType.Date)
    private OffsetDateTime updatedAt;

    // Constructors
    public HashtagDocument() {
    }

    public HashtagDocument(String id, String tag, String name, Integer postCount) {
        this.id = id;
        this.tag = tag; // Full tag with #
        this.name = name; // Tag without #
        this.postCount = postCount;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getPostCount() { return postCount; }
    public void setPostCount(Integer postCount) { this.postCount = postCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

