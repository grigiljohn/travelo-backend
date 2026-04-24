package com.travelo.circlesservice.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTagPk implements Serializable {
    private String communityId;
    private String tag;
}
