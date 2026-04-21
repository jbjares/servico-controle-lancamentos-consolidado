package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import br.com.exemplo.lancamentos.model.TipoLancamento;
import br.com.exemplo.lancamentos.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final TransactionOperations transactionOperations = new TransactionOperations() {
        @Override
        public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };

    @Test
    void devePublicarEventoComConfirmAckEMarcarComoPublicado() throws Exception {
        OutboxEvent outboxEvent = outboxEvent();
        when(repository.findById(outboxEvent.getIdEvento())).thenReturn(Optional.of(outboxEvent));
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("lancamentos.exchange"), eq("lancamento.registrado"), any(LancamentoRegistradoEvent.class), any(CorrelationData.class));

        service().publicar(outboxEvent.getIdEvento());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PUBLICADO);
        assertThat(captor.getValue().getPublicadoEm()).isNotNull();
        assertThat(captor.getValue().getUltimoErro()).isNull();
    }

    @Test
    void deveRegistrarErroEBackoffQuandoPublicacaoFalhar() throws Exception {
        OutboxEvent outboxEvent = outboxEvent();
        OffsetDateTime proximaTentativaOriginal = outboxEvent.getProximaTentativaEm();
        when(repository.findById(outboxEvent.getIdEvento())).thenReturn(Optional.of(outboxEvent));
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new AmqpException("rabbit indisponivel"))
                .when(rabbitTemplate).convertAndSend(eq("lancamentos.exchange"), eq("lancamento.registrado"), any(LancamentoRegistradoEvent.class), any(CorrelationData.class));

        service().publicar(outboxEvent.getIdEvento());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.ERRO);
        assertThat(captor.getValue().getTentativas()).isEqualTo(1);
        assertThat(captor.getValue().getUltimoErro()).contains("rabbit indisponivel");
        assertThat(captor.getValue().getProximaTentativaEm()).isAfter(proximaTentativaOriginal);
    }

    private OutboxPublisherService service() {
        return new OutboxPublisherService(repository, rabbitTemplate, objectMapper, meterRegistry, transactionOperations, 50, 5000, 1000, 2.0, 60000, "test-worker", 30000);
    }

    private OutboxEvent outboxEvent() throws Exception {
        OffsetDateTime agora = OffsetDateTime.now();
        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();
        event.setIdEvento(UUID.randomUUID());
        event.setCorrelationId("corr-123");
        event.setIdLancamento(UUID.randomUUID());
        event.setTipo(TipoLancamento.CREDITO);
        event.setValor(new BigDecimal("100.50"));
        event.setDataEfetiva(LocalDate.of(2026, 4, 18));
        event.setDescricao("Teste publisher outbox");
        event.setOcorridoEm(agora);
        event.setVersaoEvento(1);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setIdEvento(event.getIdEvento());
        outboxEvent.setTipoEvento(LancamentoRegistradoEvent.class.getSimpleName());
        outboxEvent.setCorrelationId(event.getCorrelationId());
        outboxEvent.setExchangeName("lancamentos.exchange");
        outboxEvent.setRoutingKey("lancamento.registrado");
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setStatus(OutboxStatus.PENDENTE);
        outboxEvent.setTentativas(0);
        outboxEvent.setProximaTentativaEm(agora);
        outboxEvent.setCriadoEm(agora);
        outboxEvent.setAtualizadoEm(agora);
        return outboxEvent;
    }
}
