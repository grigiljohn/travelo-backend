package com.travelo.postservice.dto;

import java.util.List;

public record UpdatePostRequest(
    String caption,
    List<String> tags,
    String location
) {
}

