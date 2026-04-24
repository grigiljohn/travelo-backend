package com.travelo.circlesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Partial update: {@code null} fields are left unchanged.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCommunityRequest {

    private String name;
    private String description;
    private String tagline;
    private String city;
    private String visibility;
    private List<String> tags;
    private List<String> topics;
    private String rules;
    private String coverImageUrl;
    private String iconImageUrl;
    private Boolean requireAdminApproval;
    private Boolean allowMemberInvites;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getIconImageUrl() {
        return iconImageUrl;
    }

    public void setIconImageUrl(String iconImageUrl) {
        this.iconImageUrl = iconImageUrl;
    }

    public Boolean getRequireAdminApproval() {
        return requireAdminApproval;
    }

    public void setRequireAdminApproval(Boolean requireAdminApproval) {
        this.requireAdminApproval = requireAdminApproval;
    }

    public Boolean getAllowMemberInvites() {
        return allowMemberInvites;
    }

    public void setAllowMemberInvites(Boolean allowMemberInvites) {
        this.allowMemberInvites = allowMemberInvites;
    }
}
