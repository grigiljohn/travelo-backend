package com.travelo.circlesservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "circle_community_tags")
@IdClass(CommunityTagPk.class)
@Getter
@Setter
@NoArgsConstructor
public class CircleCommunityTagEntity {

    @Id
    @Column(name = "community_id", length = 64)
    private String communityId;

    @Id
    @Column(length = 48)
    private String tag;
}
