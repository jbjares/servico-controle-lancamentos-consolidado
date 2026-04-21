package br.com.exemplo.consolidado.controller;

import br.com.exemplo.consolidado.dto.ConsolidadoResponse;
import br.com.exemplo.consolidado.service.ConsolidadoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/consolidados")
public class ConsolidadoController {

    private final ConsolidadoService service;

    public ConsolidadoController(ConsolidadoService service) {
        this.service = service;
    }

    @GetMapping("/{dataEfetiva}")
    public ConsolidadoResponse consultar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEfetiva) {
        return service.consultar(dataEfetiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado não encontrado"));
    }
}
