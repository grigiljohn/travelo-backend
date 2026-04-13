package com.travelo.postservice.service;

import com.travelo.postservice.dto.LocationDto;

import java.util.List;

public interface LocationService {
    List<LocationDto> searchLocations(String query);
    
    LocationDto createLocation(String name, String displayName, String address);
}

