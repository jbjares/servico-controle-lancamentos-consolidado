package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import br.com.exemplo.lancamentos.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OutboxPublisherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final TransactionOperations transactionOperations;
    private final int batchSize;
    private final long confirmTimeoutMs;
    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;
    private final String workerId;
    private final long claimTimeoutMs;

    public OutboxPublisherService(OutboxEventRepository repository,
                                  RabbitTemplate rabbitTemplate,
                                  ObjectMapper objectMapper,
                                  MeterRegistry meterRegistry,
                                  TransactionOperations transactionOperations,
                                  @Value("${app.outbox.publisher.batch-size:100}") int batchSize,
                                  @Value("${app.outbox.publisher.confirm-timeout-ms:5000}") long confirmTimeoutMs,
                                  @Value("${app.outbox.publisher.retry.initial-interval-ms:1000}") long initialIntervalMs,
                                  @Value("${app.outbox.publisher.retry.multiplier:2.0}") double multiplier,
                                  @Value("${app.outbox.publisher.retry.max-interval-ms:60000}") long maxIntervalMs,
                                  @Value("${app.outbox.publisher.worker-id:${HOSTNAME:servico-lancamentos}}") String workerId,
                                  @Value("${app.outbox.publisher.claim-timeout-ms:30000}") long claimTimeoutMs) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.transactionOperations = transactionOperations;
        this.batchSize = batchSize;
        this.confirmTimeoutMs = confirmTimeoutMs;
        this.initialIntervalMs = initialIntervalMs;
        this.multiplier = multiplier;
        this.maxIntervalMs = maxIntervalMs;
        this.workerId = workerId;
        this.claimTimeoutMs = claimTimeoutMs;
    }

    public void publicarPendentes() {
        List<UUID> idsReservados = reservarPendentes();

        if (!idsReservados.isEmpty()) {
            LOGGER.info("Lote Outbox reservado quantidade={} workerId={}", idsReservados.size(), workerId);
        }

        for (UUID idEvento : idsReservados) {
            publicar(idEvento);
        }
    }

    private List<UUID> reservarPendentes() {
        return transactionOperations.execute(status -> {
            OffsetDateTime agora = OffsetDateTime.now();
            List<OutboxEvent> eventos = repository.findDisponiveisParaReserva(
                    List.of(OutboxStatus.PENDENTE, OutboxStatus.ERRO),
                    OutboxStatus.PROCESSANDO,
                    agora,
                    PageRequest.of(0, batchSize));

            for (OutboxEvent evento : eventos) {
                evento.setStatus(OutboxStatus.PROCESSANDO);
                evento.setProcessandoPor(workerId);
                evento.setProcessandoEm(agora);
                evento.setClaimExpiraEm(agora.plusNanos(TimeUnit.MILLISECONDS.toNanos(claimTimeoutMs)));
                evento.setAtualizadoEm(agora);
            }

            repository.saveAll(eventos);
            return eventos.stream()
                    .map(OutboxEvent::getIdEvento)
                    .collect(Collectors.toList());
        });
    }

    public void publicar(UUID idEvento) {
        Optional<OutboxEvent> optional = repository.findById(idEvento);
        if (optional.isEmpty()) {
            return;
        }

        OutboxEvent outboxEvent = optional.get();
        if (outboxEvent.getStatus() == OutboxStatus.PUBLICADO) {
            return;
        }

        try {
            LancamentoRegistradoEvent event = objectMapper.readValue(outboxEvent.getPayload(), LancamentoRegistradoEvent.class);
            CorrelationData correlationData = new CorrelationData(outboxEvent.getIdEvento().toString());
            rabbitTemplate.convertAndSend(outboxEvent.getExchangeName(), outboxEvent.getRoutingKey(), event, correlationData);

            CorrelationData.Confirm confirm = correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new AmqpException("RabbitMQ retornou publisher confirm negativo: " + confirm.getReason());
            }

            marcarPublicado(outboxEvent);
        } catch (Exception e) {
            marcarErro(outboxEvent, e);
        }
    }

    private void marcarPublicado(OutboxEvent outboxEvent) {
        OffsetDateTime agora = OffsetDateTime.now();
        outboxEvent.setStatus(OutboxStatus.PUBLICADO);
        outboxEvent.setPublicadoEm(agora);
        outboxEvent.setAtualizadoEm(agora);
        outboxEvent.setProcessandoPor(null);
        outboxEvent.setProcessandoEm(null);
        outboxEvent.setClaimExpiraEm(null);
        outboxEvent.setUltimoErro(null);
        repository.save(outboxEvent);
        meterRegistry.counter("app_outbox_publish_total", "result", "published").increment();
        LOGGER.debug("Evento Outbox publicado idEvento={} correlationId={} tentativas={} workerId={}",
                outboxEvent.getIdEvento(), outboxEvent.getCorrelationId(), outboxEvent.getTentativas(), workerId);
    }

    private void marcarErro(OutboxEvent outboxEvent, Exception exception) {
        OffsetDateTime agora = OffsetDateTime.now();
        int tentativas = outboxEvent.getTentativas() == null ? 1 : outboxEvent.getTentativas() + 1;
        outboxEvent.setStatus(OutboxStatus.ERRO);
        outboxEvent.setTentativas(tentativas);
        outboxEvent.setAtualizadoEm(agora);
        outboxEvent.setProcessandoPor(null);
        outboxEvent.setProcessandoEm(null);
        outboxEvent.setClaimExpiraEm(null);
        outboxEvent.setProximaTentativaEm(agora.plusNanos(TimeUnit.MILLISECONDS.toNanos(calcularBackoffMs(tentativas))));
        outboxEvent.setUltimoErro(limitarMensagem(exception));
        repository.save(outboxEvent);
        meterRegistry.counter("app_outbox_publish_total", "result", "error").increment();
        LOGGER.warn("Falha ao publicar evento outbox idEvento={}, tentativa={}, proximaTentativaEm={}, workerId={}, erro={}",
                outboxEvent.getIdEvento(), tentativas, outboxEvent.getProximaTentativaEm(), workerId, limitarMensagem(exception));
    }

    private long calcularBackoffMs(int tentativas) {
        double intervalo = initialIntervalMs * Math.pow(multiplier, Math.max(0, tentativas - 1));
        return Math.min(maxIntervalMs, Math.round(intervalo));
    }

    private String limitarMensagem(Exception exception) {
        String mensagem = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        if (mensagem.length() <= 1000) {
            return mensagem;
        }
        return mensagem.substring(0, 1000);
    }
}
