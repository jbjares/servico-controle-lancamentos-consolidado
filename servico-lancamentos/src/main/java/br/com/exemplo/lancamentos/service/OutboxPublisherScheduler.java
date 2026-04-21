package br.com.exemplo.lancamentos.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherScheduler {

    private final OutboxPublisherService publisherService;

    public OutboxPublisherScheduler(OutboxPublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @Scheduled(
            initialDelayString = "${app.outbox.publisher.initial-delay-ms:2000}",
            fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:500}")
    public void publicarPendentes() {
        publisherService.publicarPendentes();
    }
}
