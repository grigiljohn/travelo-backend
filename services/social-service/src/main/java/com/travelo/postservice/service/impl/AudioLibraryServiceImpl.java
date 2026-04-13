package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.AudioLibraryDto;
import com.travelo.postservice.entity.AudioLibrary;
import com.travelo.postservice.repository.AudioLibraryRepository;
import com.travelo.postservice.service.AudioLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AudioLibraryServiceImpl implements AudioLibraryService {
    private static final Logger logger = LoggerFactory.getLogger(AudioLibraryServiceImpl.class);
    
    private final AudioLibraryRepository audioLibraryRepository;
    
    public AudioLibraryServiceImpl(AudioLibraryRepository audioLibraryRepository) {
        this.audioLibraryRepository = audioLibraryRepository;
    }
    
    @Override
    public List<AudioLibraryDto> getAllAudio() {
        return audioLibraryRepository.findByIsActiveTrueOrderByName().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AudioLibraryDto> getAudioByCategory(String category) {
        return audioLibraryRepository.findByCategoryAndIsActiveTrue(category).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private AudioLibraryDto toDto(AudioLibrary audio) {
        return new AudioLibraryDto(
            audio.getId(),
            audio.getName(),
            audio.getFileUrl(),
            audio.getDurationSeconds(),
            audio.getCategory(),
            audio.getThumbnailUrl()
        );
    }
}

