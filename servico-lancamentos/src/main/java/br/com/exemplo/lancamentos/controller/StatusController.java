package br.com.exemplo.lancamentos.controller;

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
        body.put("servico", "servico-lancamentos");
        body.put("status", "UP");
        body.put("timestamp", OffsetDateTime.now());
        body.put("endpoints", Map.of(
                "criarLancamento", "POST /api/lancamentos",
                "consultarLancamentos", "GET /api/lancamentos?dataInicio=YYYY-MM-DD&dataFim=YYYY-MM-DD",
                "listarLancamentos", "GET /api/lancamentos"
        ));
        return body;
    }
}