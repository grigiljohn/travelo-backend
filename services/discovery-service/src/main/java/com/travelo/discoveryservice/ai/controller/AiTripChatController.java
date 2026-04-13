package com.travelo.discoveryservice.ai.controller;

import com.travelo.discoveryservice.ai.dto.AiTripChatRequest;
import com.travelo.discoveryservice.ai.dto.AiTripChatResponse;
import com.travelo.discoveryservice.ai.service.AiTripOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/ai/trip")
@Tag(name = "AI Trip", description = "Conversational trip planning (stub / hardcoded responses)")
public class AiTripChatController {

    private final AiTripOrchestratorService orchestrator;

    public AiTripChatController(AiTripOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    @Operation(summary = "Trip assistant turn", description = "Merges user message into slots; asks next missing field or returns a sample itinerary.")
    public ResponseEntity<AiTripChatResponse> chat(@RequestBody AiTripChatRequest request) {
        return ResponseEntity.ok(orchestrator.chat(request));
    }
}
