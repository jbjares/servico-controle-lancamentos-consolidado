package br.com.exemplo.lancamentos.config;

import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import br.com.exemplo.lancamentos.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Configuration
public class OutboxMetricsConfig {

    public OutboxMetricsConfig(MeterRegistry meterRegistry, OutboxEventRepository repository) {
        for (OutboxStatus status : OutboxStatus.values()) {
            Gauge.builder("app_outbox_events", repository, repo -> repo.countByStatus(status))
                    .description("Quantidade de eventos no Outbox por status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        Gauge.builder("app_outbox_oldest_pending_age_seconds", repository, this::idadeEventoPendenteMaisAntigo)
                .description("Idade em segundos do evento pendente/erro mais antigo no Outbox")
                .register(meterRegistry);
    }

    private double idadeEventoPendenteMaisAntigo(OutboxEventRepository repository) {
        Optional<OutboxEvent> evento = repository.findFirstByStatusInOrderByCriadoEmAsc(
                List.of(OutboxStatus.PENDENTE, OutboxStatus.ERRO, OutboxStatus.PROCESSANDO));
        return evento.map(outboxEvent -> Math.max(0, Duration.between(
                        outboxEvent.getCriadoEm(), OffsetDateTime.now()).getSeconds()))
                .orElse(0L)
                .doubleValue();
    }
}
