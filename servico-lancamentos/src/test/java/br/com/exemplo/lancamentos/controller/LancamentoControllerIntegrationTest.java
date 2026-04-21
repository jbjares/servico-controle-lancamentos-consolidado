package br.com.exemplo.lancamentos.controller;

import br.com.exemplo.lancamentos.dto.LancamentoRequest;
import br.com.exemplo.lancamentos.model.TipoLancamento;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LancamentoControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void deveExporStatusNaRaiz() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("servico-lancamentos", "GET /api/lancamentos");
    }

    @Test
    void deveCriarEConsultarLancamentoComH2() {
        ResponseEntity<String> criarResponse = restTemplate.postForEntity("/api/lancamentos", request(), String.class);
        assertThat(criarResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criarResponse.getBody()).contains("RECEBIDO");

        ResponseEntity<String> listarTodosResponse = restTemplate.getForEntity("/api/lancamentos", String.class);
        assertThat(listarTodosResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listarTodosResponse.getBody()).contains("Teste H2", "CREDITO");

        ResponseEntity<String> filtrarResponse = restTemplate.getForEntity(
                "/api/lancamentos?dataInicio=2026-04-18&dataFim=2026-04-18", String.class);
        assertThat(filtrarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtrarResponse.getBody()).contains("Teste H2", "100.50");
    }

    private LancamentoRequest request() {
        LancamentoRequest request = new LancamentoRequest();
        request.setTipo(TipoLancamento.CREDITO);
        request.setValor(new BigDecimal("100.50"));
        request.setDataEfetiva(LocalDate.of(2026, 4, 18));
        request.setDescricao("Teste H2");
        return request;
    }
}