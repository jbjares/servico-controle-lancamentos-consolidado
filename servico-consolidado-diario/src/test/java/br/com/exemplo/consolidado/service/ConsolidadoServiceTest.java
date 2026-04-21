package br.com.exemplo.consolidado.service;

import br.com.exemplo.consolidado.event.LancamentoRegistradoEvent;
import br.com.exemplo.consolidado.model.ConsolidadoDiario;
import br.com.exemplo.consolidado.model.EventoProcessado;
import br.com.exemplo.consolidado.model.TipoLancamento;
import br.com.exemplo.consolidado.repository.ConsolidadoDiarioRepository;
import br.com.exemplo.consolidado.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsolidadoServiceTest {

    @Mock
    private ConsolidadoDiarioRepository consolidadoRepository;

    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    @InjectMocks
    private ConsolidadoService service;

    @Test
    void deveProcessarCreditoComIdempotenciaRegistrada() {
        LancamentoRegistradoEvent event = event(TipoLancamento.CREDITO, "100.50");
        when(eventoProcessadoRepository.existsById(event.getIdEvento())).thenReturn(false);
        when(consolidadoRepository.findById(event.getDataEfetiva())).thenReturn(Optional.empty());
        when(consolidadoRepository.save(any(ConsolidadoDiario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processarEvento(event);

        ArgumentCaptor<ConsolidadoDiario> consolidadoCaptor = ArgumentCaptor.forClass(ConsolidadoDiario.class);
        verify(consolidadoRepository).save(consolidadoCaptor.capture());
        assertThat(consolidadoCaptor.getValue().getTotalCreditos()).isEqualByComparingTo("100.50");
        assertThat(consolidadoCaptor.getValue().getTotalDebitos()).isEqualByComparingTo("0.00");
        assertThat(consolidadoCaptor.getValue().getSaldoFinal()).isEqualByComparingTo("100.50");

        ArgumentCaptor<EventoProcessado> eventoCaptor = ArgumentCaptor.forClass(EventoProcessado.class);
        verify(eventoProcessadoRepository).save(eventoCaptor.capture());
        assertThat(eventoCaptor.getValue().getIdEvento()).isEqualTo(event.getIdEvento());
    }

    @Test
    void deveIgnorarEventoJaProcessado() {
        LancamentoRegistradoEvent event = event(TipoLancamento.DEBITO, "25.00");
        when(eventoProcessadoRepository.existsById(event.getIdEvento())).thenReturn(true);

        service.processarEvento(event);

        verify(consolidadoRepository, never()).save(any());
        verify(eventoProcessadoRepository, never()).save(any());
    }

    @Test
    void deveAtualizarSaldoComDebito() {
        LancamentoRegistradoEvent event = event(TipoLancamento.DEBITO, "25.00");
        ConsolidadoDiario existente = new ConsolidadoDiario();
        existente.setDataEfetiva(event.getDataEfetiva());
        existente.setTotalCreditos(new BigDecimal("100.00"));
        existente.setTotalDebitos(BigDecimal.ZERO);
        existente.setSaldoFinal(new BigDecimal("100.00"));
        existente.setUltimaAtualizacao(java.time.OffsetDateTime.now());
        when(eventoProcessadoRepository.existsById(event.getIdEvento())).thenReturn(false);
        when(consolidadoRepository.findById(event.getDataEfetiva())).thenReturn(Optional.of(existente));
        when(consolidadoRepository.save(any(ConsolidadoDiario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processarEvento(event);

        ArgumentCaptor<ConsolidadoDiario> consolidadoCaptor = ArgumentCaptor.forClass(ConsolidadoDiario.class);
        verify(consolidadoRepository).save(consolidadoCaptor.capture());
        assertThat(consolidadoCaptor.getValue().getTotalDebitos()).isEqualByComparingTo("25.00");
        assertThat(consolidadoCaptor.getValue().getSaldoFinal()).isEqualByComparingTo("75.00");
    }

    private LancamentoRegistradoEvent event(TipoLancamento tipo, String valor) {
        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();
        event.setIdEvento(UUID.randomUUID());
        event.setCorrelationId("corr-test");
        event.setIdLancamento(UUID.randomUUID());
        event.setTipo(tipo);
        event.setValor(new BigDecimal(valor));
        event.setDataEfetiva(LocalDate.of(2026, 4, 18));
        event.setDescricao("Teste automatizado");
        event.setOcorridoEm(java.time.OffsetDateTime.now());
        event.setVersaoEvento(1);
        return event;
    }
}