package br.com.exemplo.consolidado.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("servico", "servico-consolidado-diario");
        body.put("status", "UP");
        body.put("timestamp", OffsetDateTime.now());
        body.put("endpoints", Map.of(
                "consultarConsolidado", "GET /api/consolidados/{dataEfetiva}"
        ));
        return body;
    }
}