package br.com.exemplo.consolidado.controller;

import br.com.exemplo.consolidado.event.LancamentoRegistradoEvent;
import br.com.exemplo.consolidado.model.TipoLancamento;
import br.com.exemplo.consolidado.service.ConsolidadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConsolidadoControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConsolidadoService service;

    @Test
    void deveExporStatusNaRaiz() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("servico-consolidado-diario", "GET /api/consolidados/{dataEfetiva}");
    }

    @Test
    void deveConsultarConsolidadoProcessadoComH2() {
        service.processarEvento(event(TipoLancamento.CREDITO, "100.50", UUID.randomUUID()));
        service.processarEvento(event(TipoLancamento.DEBITO, "20.00", UUID.randomUUID()));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/consolidados/2026-04-18", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("100.50", "20.00", "80.50");
    }

    @Test
    void deveRetornar404QuandoConsolidadoNaoExiste() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/consolidados/2026-04-19", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("NAO_ENCONTRADO");
    }

    private LancamentoRegistradoEvent event(TipoLancamento tipo, String valor, UUID idEvento) {
        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();
        event.setIdEvento(idEvento);
        event.setCorrelationId("corr-test");
        event.setIdLancamento(UUID.randomUUID());
        event.setTipo(tipo);
        event.setValor(new BigDecimal(valor));
        event.setDataEfetiva(LocalDate.of(2026, 4, 18));
        event.setDescricao("Teste H2");
        event.setOcorridoEm(OffsetDateTime.now());
        event.setVersaoEvento(1);
        return event;
    }
}