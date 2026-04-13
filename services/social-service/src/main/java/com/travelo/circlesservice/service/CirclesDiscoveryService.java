package com.travelo.circlesservice.service;

import com.travelo.circlesservice.dto.CirclesDiscoveryDto;
import com.travelo.circlesservice.dto.NearTravelerDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CirclesDiscoveryService {

    /**
     * In-memory seed (replace with geo / user-service query later).
     *
     * @param city client-reported locality from device; echoed in the response for UI consistency.
     */
    public CirclesDiscoveryDto getDiscovery(String city) {
        String displayCity = (city == null || city.isBlank()) ? "Kuala Lumpur" : city.trim();
        List<NearTravelerDto> people = List.of(
                new NearTravelerDto(
                        "u1",
                        "Arjun",
                        "https://picsum.photos/seed/circle-arjun/200/200",
                        "Solo traveler",
                        List.of("Food", "Culture"),
                        "1.2 km away",
                        "Weekend explorer · always hunting for the best nasi lemak.",
                        14
                ),
                new NearTravelerDto(
                        "u2",
                        "Maya",
                        "https://picsum.photos/seed/circle-maya/200/200",
                        "Digital nomad",
                        List.of("Coffee", "Hiking"),
                        "800 m away",
                        "Remote designer · love sunrise hikes before calls.",
                        28
                ),
                new NearTravelerDto(
                        "u3",
                        "Jon",
                        "https://picsum.photos/seed/circle-jon/200/200",
                        "Backpacker",
                        List.of("Night markets", "Street food"),
                        "2.4 km away",
                        "On a 6-month Asia loop. KL stop #3.",
                        9
                ),
                new NearTravelerDto(
                        "u4",
                        "Sofia",
                        "https://picsum.photos/seed/circle-sofia/200/200",
                        "Photographer",
                        List.of("Architecture", "Art"),
                        "3.1 km away",
                        "Shooting golden hour everywhere I land.",
                        31
                )
        );
        return new CirclesDiscoveryDto(displayCity, 23, people);
    }
}
