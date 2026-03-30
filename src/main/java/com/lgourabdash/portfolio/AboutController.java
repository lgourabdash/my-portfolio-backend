package com.lgourabdash.portfolio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class AboutController {

    private final AboutService aboutService;

    public AboutController(AboutService aboutService) {
        this.aboutService = aboutService;
    }

    @GetMapping("/about")
    public ResponseEntity<String> getAbout() {
        String content = aboutService.getAbout().getContent();
        return ResponseEntity.ok(content == null ? "" : content);
    }

    @PostMapping("/about")
    public ResponseEntity<String> updateAbout(@RequestBody String content) {
        aboutService.updateAbout(content);
        return ResponseEntity.ok("About section updated successfully!");
    }
}
