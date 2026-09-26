package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to Benh So An API v1",
                "status", "running",
                "timestamp", Instant.now().toString()
        ));
    }
}
