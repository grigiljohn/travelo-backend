package com.travelo.discoveryservice.ai.controller;

import com.travelo.discoveryservice.ai.dto.AiTripChatRequest;
import com.travelo.discoveryservice.ai.dto.AiTripChatResponse;
import com.travelo.discoveryservice.ai.dto.BuildItineraryRequest;
import com.travelo.discoveryservice.ai.dto.BuildItineraryResponse;
import com.travelo.discoveryservice.ai.service.AiTripOrchestratorService;
import com.travelo.discoveryservice.ai.service.BuildItineraryService;
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
    private final BuildItineraryService buildItineraryService;

    public AiTripChatController(
            AiTripOrchestratorService orchestrator,
            BuildItineraryService buildItineraryService) {
        this.orchestrator = orchestrator;
        this.buildItineraryService = buildItineraryService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Trip assistant turn", description = "Merges user message into slots; asks next missing field or returns a sample itinerary.")
    public ResponseEntity<AiTripChatResponse> chat(@RequestBody AiTripChatRequest request) {
        return ResponseEntity.ok(orchestrator.chat(request));
    }

    @PostMapping("/build-itinerary")
    @Operation(
            summary = "Build structured itinerary",
            description = "Generates day-by-day stops via OpenAI (JSON) or a template when OpenAI is disabled or fails. "
                    + "Used by the mobile “Build my itinerary” flow after activities selection.")
    public ResponseEntity<BuildItineraryResponse> buildItinerary(@RequestBody BuildItineraryRequest request) {
        return ResponseEntity.ok(buildItineraryService.build(request));
    }
}
