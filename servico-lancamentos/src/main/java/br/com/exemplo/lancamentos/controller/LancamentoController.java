package br.com.exemplo.lancamentos.controller;

import br.com.exemplo.lancamentos.dto.LancamentoRequest;
import br.com.exemplo.lancamentos.dto.LancamentoResponse;
import br.com.exemplo.lancamentos.model.Lancamento;
import br.com.exemplo.lancamentos.service.LancamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

    private final LancamentoService service;

    public LancamentoController(LancamentoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse criar(@RequestBody LancamentoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public List<Lancamento> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.consultar(dataInicio, dataFim);
    }
}