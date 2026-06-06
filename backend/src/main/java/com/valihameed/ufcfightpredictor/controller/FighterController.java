package com.valihameed.UFCFightPredictor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/fighters")
public class FighterController {

    @GetMapping
    public ResponseEntity<String> getFighters() {
        try {
            Path path = Paths.get("data/fighters.json");
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path));
                return ResponseEntity.ok().header("Content-Type", "application/json").body(content);
            } else {
                return ResponseEntity.ok().header("Content-Type", "application/json").body("{\"Active\": {}, \"Inactive\": {}}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).header("Content-Type", "application/json").body("{\"Active\": {}, \"Inactive\": {}}");
        }
    }
}
