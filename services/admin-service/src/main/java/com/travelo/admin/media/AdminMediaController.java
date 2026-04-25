package com.travelo.admin.media;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.media.dto.AdminImageUploadResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/media")
public class AdminMediaController {

    private final AdminMediaUploadProxyService mediaProxy;

    public AdminMediaController(AdminMediaUploadProxyService mediaProxy) {
        this.mediaProxy = mediaProxy;
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminImageUploadResult>> uploadImage(
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) throws IOException {
        String ownerHint = auth != null ? auth.getName() : null;
        AdminImageUploadResult result = mediaProxy.uploadImage(file, ownerHint);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Image uploaded", result));
    }
}
