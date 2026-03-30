package com.lgourabdash.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PortfolioSectionController {

    private final PortfolioSectionService sectionService;
    private final AdminKeyService adminKeyService;

    public PortfolioSectionController(
            PortfolioSectionService sectionService, AdminKeyService adminKeyService) {
        this.sectionService = sectionService;
        this.adminKeyService = adminKeyService;
    }

    /** Public: all section JSON merged (navbar, contact, skills, etc.). */
    @GetMapping("/api/public/sections")
    public ResponseEntity<Map<String, JsonNode>> getPublicSections() {
        return ResponseEntity.ok(sectionService.getAllAsMap());
    }

    @PutMapping("/api/admin/sections/{key}")
    public ResponseEntity<String> upsertSection(
            @PathVariable String key,
            @RequestBody JsonNode body,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!adminKeyService.authorize(adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
        }
        try {
            sectionService.upsertSection(key.trim().toLowerCase(), body);
            return ResponseEntity.ok("Saved section: " + key);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
