package com.travelo.postservice.service;

import com.travelo.postservice.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaUploadResponse uploadMedia(MultipartFile file, String mediaType);
}

