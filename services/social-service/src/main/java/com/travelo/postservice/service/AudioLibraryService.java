package com.travelo.postservice.service;

import com.travelo.postservice.dto.AudioLibraryDto;

import java.util.List;

public interface AudioLibraryService {
    List<AudioLibraryDto> getAllAudio();
    
    List<AudioLibraryDto> getAudioByCategory(String category);
}

