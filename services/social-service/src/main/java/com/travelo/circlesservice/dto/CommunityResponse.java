package com.travelo.circlesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityResponse {

    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private String visibility;
    private String city;
    private int memberCount;
    private String lastActivity;
    private boolean member;
    private boolean owner;

    public static CommunityResponse of(
            String id,
            String name,
            String description,
            List<String> tags,
            String visibility,
            String city,
            int memberCount,
            String lastActivity,
            boolean member,
            boolean owner
    ) {
        CommunityResponse r = new CommunityResponse();
        r.setId(id);
        r.setName(name);
        r.setDescription(description);
        r.setTags(tags);
        r.setVisibility(visibility);
        r.setCity(city);
        r.setMemberCount(memberCount);
        r.setLastActivity(lastActivity);
        r.setMember(member);
        r.setOwner(owner);
        return r;
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
