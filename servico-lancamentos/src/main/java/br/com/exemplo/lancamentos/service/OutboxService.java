package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import br.com.exemplo.lancamentos.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class OutboxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent registrarLancamentoRegistrado(LancamentoRegistradoEvent event, String exchange, String routingKey) {
        OffsetDateTime agora = OffsetDateTime.now();
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setIdEvento(event.getIdEvento());
        outboxEvent.setTipoEvento(LancamentoRegistradoEvent.class.getSimpleName());
        outboxEvent.setCorrelationId(event.getCorrelationId());
        outboxEvent.setExchangeName(exchange);
        outboxEvent.setRoutingKey(routingKey);
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setStatus(OutboxStatus.PENDENTE);
        outboxEvent.setTentativas(0);
        outboxEvent.setProximaTentativaEm(agora);
        outboxEvent.setCriadoEm(agora);
        outboxEvent.setAtualizadoEm(agora);
        OutboxEvent salvo = repository.save(outboxEvent);
        LOGGER.debug("Evento registrado no Outbox idEvento={} correlationId={} routingKey={}",
                salvo.getIdEvento(), salvo.getCorrelationId(), salvo.getRoutingKey());
        return salvo;
    }

    private String toJson(LancamentoRegistradoEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nao foi possivel serializar evento para outbox", e);
        }
    }
}
