package com.travelo.searchservice;

import com.travelo.commons.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SearchServiceHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return HealthResponse.ok("discovery-service");
    }
}
