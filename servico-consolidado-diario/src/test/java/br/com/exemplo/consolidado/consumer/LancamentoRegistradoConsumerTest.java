package br.com.exemplo.consolidado.consumer;

import br.com.exemplo.consolidado.event.LancamentoRegistradoEvent;
import br.com.exemplo.consolidado.service.ConsolidadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LancamentoRegistradoConsumerTest {

    @Mock
    private ConsolidadoService consolidadoService;

    @Test
    void deveDelegarEventoParaServicoDeConsolidado() {
        LancamentoRegistradoConsumer consumer = new LancamentoRegistradoConsumer(consolidadoService);
        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();

        consumer.consumir(event);

        verify(consolidadoService).processarEvento(event);
    }
}