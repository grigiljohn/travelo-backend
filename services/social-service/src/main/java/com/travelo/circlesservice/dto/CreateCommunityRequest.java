package com.travelo.circlesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateCommunityRequest {

    private String name;
    private String description;
    private String tagline;
    private List<String> tags = new ArrayList<>();
    private List<String> topics = new ArrayList<>();
    private String rules;
    private String coverImageUrl;
    private String iconImageUrl;
    /** "public", "private", or "secret" (invite-only, not promoted in discovery). */
    private String visibility;
    private String city;
    /** User ids to add as members at creation time (friends circle). */
    private List<String> inviteUserIds = new ArrayList<>();
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

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics != null ? topics : new ArrayList<>();
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getInviteUserIds() {
        return inviteUserIds;
    }

    public void setInviteUserIds(List<String> inviteUserIds) {
        this.inviteUserIds = inviteUserIds != null ? inviteUserIds : new ArrayList<>();
    }
}
