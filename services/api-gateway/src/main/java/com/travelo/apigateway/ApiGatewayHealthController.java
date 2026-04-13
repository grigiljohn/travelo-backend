package com.travelo.apigateway;

import com.travelo.commons.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiGatewayHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return HealthResponse.ok("api-gateway");
    }
}
