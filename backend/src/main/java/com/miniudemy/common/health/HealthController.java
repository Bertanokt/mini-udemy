package com.miniudemy.common.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontend ve docker-compose'un "backend ayakta mı" kontrolü için.
 * Altyapı detayları (DB, disk) için /actuator/health kullanılır.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mini-udemy-backend");
    }
}
