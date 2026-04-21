package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import br.com.exemplo.lancamentos.model.TipoLancamento;
import br.com.exemplo.lancamentos.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Test
    void devePersistirEventoOutboxPendenteComPayloadJson() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OutboxService service = new OutboxService(repository, objectMapper);
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent salvo = service.registrarLancamentoRegistrado(evento(), "lancamentos.exchange", "lancamento.registrado");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(salvo.getStatus()).isEqualTo(OutboxStatus.PENDENTE);
        assertThat(salvo.getTentativas()).isZero();
        assertThat(salvo.getExchangeName()).isEqualTo("lancamentos.exchange");
        assertThat(salvo.getRoutingKey()).isEqualTo("lancamento.registrado");
        assertThat(captor.getValue().getPayload()).contains("idLancamento", "CREDITO", "100.50");
    }

    private LancamentoRegistradoEvent evento() {
        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();
        event.setIdEvento(UUID.randomUUID());
        event.setCorrelationId("corr-123");
        event.setIdLancamento(UUID.randomUUID());
        event.setTipo(TipoLancamento.CREDITO);
        event.setValor(new BigDecimal("100.50"));
        event.setDataEfetiva(LocalDate.of(2026, 4, 18));
        event.setDescricao("Teste outbox");
        event.setOcorridoEm(OffsetDateTime.now());
        event.setVersaoEvento(1);
        return event;
    }
}
