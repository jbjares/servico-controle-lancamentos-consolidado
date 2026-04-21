package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.dto.LancamentoRequest;
import br.com.exemplo.lancamentos.dto.LancamentoResponse;
import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.Lancamento;
import br.com.exemplo.lancamentos.model.TipoLancamento;
import br.com.exemplo.lancamentos.repository.LancamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock
    private LancamentoRepository repository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private LancamentoService service;

    @Test
    void deveRegistrarLancamentoERegistrarEventoNoOutbox() {
        ReflectionTestUtils.setField(service, "exchange", "lancamentos.exchange");
        ReflectionTestUtils.setField(service, "routingKey", "lancamento.registrado");
        when(repository.save(any(Lancamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LancamentoResponse response = service.registrar(request(TipoLancamento.CREDITO, "100.50", LocalDate.of(2026, 4, 18)));

        ArgumentCaptor<Lancamento> lancamentoCaptor = ArgumentCaptor.forClass(Lancamento.class);
        verify(repository).save(lancamentoCaptor.capture());
        assertThat(lancamentoCaptor.getValue().getId()).isNotNull();
        assertThat(lancamentoCaptor.getValue().getValor()).isEqualByComparingTo("100.50");
        assertThat(response.getStatus()).isEqualTo("RECEBIDO");

        ArgumentCaptor<LancamentoRegistradoEvent> eventCaptor = ArgumentCaptor.forClass(LancamentoRegistradoEvent.class);
        verify(outboxService).registrarLancamentoRegistrado(
                eventCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("lancamentos.exchange"),
                org.mockito.ArgumentMatchers.eq("lancamento.registrado"));
        assertThat(eventCaptor.getValue().getIdEvento()).isNotNull();
        assertThat(eventCaptor.getValue().getTipo()).isEqualTo(TipoLancamento.CREDITO);
        assertThat(eventCaptor.getValue().getValor()).isEqualByComparingTo("100.50");
    }

    @Test
    void deveRejeitarLancamentoComValorInvalido() {
        assertThatThrownBy(() -> service.registrar(request(TipoLancamento.DEBITO, "0.00", LocalDate.of(2026, 4, 18))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void deveConsultarTodosQuandoPeriodoNaoForInformado() {
        Lancamento lancamento = new Lancamento();
        when(repository.findAll()).thenReturn(List.of(lancamento));

        assertThat(service.consultar(null, null)).containsExactly(lancamento);
    }

    @Test
    void deveRejeitarPeriodoParcial() {
        assertThatThrownBy(() -> service.consultar(LocalDate.of(2026, 4, 18), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataInicio e dataFim");
    }

    private LancamentoRequest request(TipoLancamento tipo, String valor, LocalDate dataEfetiva) {
        LancamentoRequest request = new LancamentoRequest();
        request.setTipo(tipo);
        request.setValor(new BigDecimal(valor));
        request.setDataEfetiva(dataEfetiva);
        request.setDescricao("Teste automatizado");
        return request;
    }
}