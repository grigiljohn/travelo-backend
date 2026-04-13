package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ad_targeting")
public class AdTargeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    // Demographics
    @ElementCollection
    @CollectionTable(name = "targeting_age_groups", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "age_group")
    private List<String> ageGroups = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "targeting_genders", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "gender")
    private List<String> genders = new ArrayList<>();

    // Location
    @ElementCollection
    @CollectionTable(name = "targeting_locations", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "location")
    private List<String> locations = new ArrayList<>();

    // Interests
    @ElementCollection
    @CollectionTable(name = "targeting_interests", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "interest")
    private List<String> interests = new ArrayList<>();

    // Behaviors
    @ElementCollection
    @CollectionTable(name = "targeting_behaviors", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "behavior")
    private List<String> behaviors = new ArrayList<>();

    // Custom audiences
    @ElementCollection
    @CollectionTable(name = "targeting_custom_audiences", joinColumns = @JoinColumn(name = "targeting_id"))
    @Column(name = "audience_id")
    private List<String> customAudiences = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ad getAd() {
        return ad;
    }

    public void setAd(Ad ad) {
        this.ad = ad;
    }

    public List<String> getAgeGroups() {
        return ageGroups;
    }

    public void setAgeGroups(List<String> ageGroups) {
        this.ageGroups = ageGroups;
    }

    public List<String> getGenders() {
        return genders;
    }

    public void setGenders(List<String> genders) {
        this.genders = genders;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public List<String> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<String> behaviors) {
        this.behaviors = behaviors;
    }

    public List<String> getCustomAudiences() {
        return customAudiences;
    }

    public void setCustomAudiences(List<String> customAudiences) {
        this.customAudiences = customAudiences;
    }
}

