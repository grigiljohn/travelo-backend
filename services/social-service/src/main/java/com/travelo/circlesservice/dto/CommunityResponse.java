package com.travelo.circlesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityResponse {

    private String id;
    private String name;
    private String description;
    private String tagline;
    private List<String> tags;
    private List<String> topics;
    private String rules;
    private String coverImageUrl;
    private String iconImageUrl;
    private String visibility;
    private String city;
    private int memberCount;
    private String lastActivity;
    private boolean member;
    private boolean owner;
    private boolean pendingJoinRequest;
    private boolean requireAdminApproval;
    private boolean allowMemberInvites;

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

    public boolean isPendingJoinRequest() {
        return pendingJoinRequest;
    }

    public void setPendingJoinRequest(boolean pendingJoinRequest) {
        this.pendingJoinRequest = pendingJoinRequest;
    }

    public boolean isRequireAdminApproval() {
        return requireAdminApproval;
    }

    public void setRequireAdminApproval(boolean requireAdminApproval) {
        this.requireAdminApproval = requireAdminApproval;
    }

    public boolean isAllowMemberInvites() {
        return allowMemberInvites;
    }

    public void setAllowMemberInvites(boolean allowMemberInvites) {
        this.allowMemberInvites = allowMemberInvites;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isMember() {
        return member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}
