package com.travelo.adservice.dto;

import java.util.List;

public record AdTargetingDto(
    List<String> ageGroups,
    List<String> genders,
    List<String> locations,
    List<String> interests,
    List<String> behaviors,
    List<String> customAudiences
) {
}

