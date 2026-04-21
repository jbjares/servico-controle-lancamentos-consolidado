package br.com.exemplo.consolidado.consumer;

import br.com.exemplo.consolidado.event.LancamentoRegistradoEvent;
import br.com.exemplo.consolidado.service.ConsolidadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class LancamentoRegistradoConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoRegistradoConsumer.class);

    private final ConsolidadoService consolidadoService;

    public LancamentoRegistradoConsumer(ConsolidadoService consolidadoService) {
        this.consolidadoService = consolidadoService;
    }

    @RabbitListener(queues = "${app.mensageria.fila}")
    public void consumir(LancamentoRegistradoEvent event) {
        LOGGER.debug("Evento recebido idEvento={} correlationId={} idLancamento={}",
                event.getIdEvento(), event.getCorrelationId(), event.getIdLancamento());
        try {
            consolidadoService.processarEvento(event);
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao consumir evento idEvento={} correlationId={}",
                    event.getIdEvento(), event.getCorrelationId(), ex);
            throw ex;
        }
    }
}
