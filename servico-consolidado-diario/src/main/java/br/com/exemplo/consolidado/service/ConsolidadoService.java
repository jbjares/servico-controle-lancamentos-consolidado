package br.com.exemplo.consolidado.service;

import br.com.exemplo.consolidado.dto.ConsolidadoResponse;
import br.com.exemplo.consolidado.event.LancamentoRegistradoEvent;
import br.com.exemplo.consolidado.model.ConsolidadoDiario;
import br.com.exemplo.consolidado.model.EventoProcessado;
import br.com.exemplo.consolidado.model.TipoLancamento;
import br.com.exemplo.consolidado.repository.ConsolidadoDiarioRepository;
import br.com.exemplo.consolidado.repository.EventoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ConsolidadoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsolidadoService.class);

    private final ConsolidadoDiarioRepository consolidadoRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;

    public ConsolidadoService(ConsolidadoDiarioRepository consolidadoRepository,
                              EventoProcessadoRepository eventoProcessadoRepository) {
        this.consolidadoRepository = consolidadoRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void processarEvento(LancamentoRegistradoEvent event) {
        if (eventoProcessadoRepository.existsById(event.getIdEvento())) {
            LOGGER.info("Evento duplicado ignorado idEvento={} correlationId={}", event.getIdEvento(), event.getCorrelationId());
            return;
        }

        LocalDate data = event.getDataEfetiva();
        ConsolidadoDiario consolidado = consolidadoRepository.findById(data)
                .orElseGet(() -> novoConsolidado(data));

        if (event.getTipo() == TipoLancamento.CREDITO) {
            consolidado.setTotalCreditos(consolidado.getTotalCreditos().add(event.getValor()));
        } else {
            consolidado.setTotalDebitos(consolidado.getTotalDebitos().add(event.getValor()));
        }

        consolidado.setSaldoFinal(consolidado.getTotalCreditos().subtract(consolidado.getTotalDebitos()));
        consolidado.setUltimaAtualizacao(OffsetDateTime.now());
        consolidadoRepository.save(consolidado);

        EventoProcessado eventoProcessado = new EventoProcessado();
        eventoProcessado.setIdEvento(event.getIdEvento());
        eventoProcessado.setCorrelationId(event.getCorrelationId());
        eventoProcessado.setProcessadoEm(OffsetDateTime.now());
        eventoProcessadoRepository.save(eventoProcessado);
        LOGGER.info("Consolidado atualizado idEvento={} dataEfetiva={} tipo={} valor={} saldoFinal={}",
                event.getIdEvento(), data, event.getTipo(), event.getValor(), consolidado.getSaldoFinal());
    }

    public Optional<ConsolidadoResponse> consultar(LocalDate dataEfetiva) {
        return consolidadoRepository.findById(dataEfetiva)
                .map(this::toResponse);
    }

    private ConsolidadoDiario novoConsolidado(LocalDate data) {
        ConsolidadoDiario consolidado = new ConsolidadoDiario();
        consolidado.setDataEfetiva(data);
        consolidado.setTotalCreditos(BigDecimal.ZERO);
        consolidado.setTotalDebitos(BigDecimal.ZERO);
        consolidado.setSaldoFinal(BigDecimal.ZERO);
        consolidado.setUltimaAtualizacao(OffsetDateTime.now());
        return consolidado;
    }

    private ConsolidadoResponse toResponse(ConsolidadoDiario consolidado) {
        ConsolidadoResponse response = new ConsolidadoResponse();
        response.setDataEfetiva(consolidado.getDataEfetiva());
        response.setTotalCreditos(consolidado.getTotalCreditos());
        response.setTotalDebitos(consolidado.getTotalDebitos());
        response.setSaldoFinal(consolidado.getSaldoFinal());
        response.setUltimaAtualizacao(consolidado.getUltimaAtualizacao());
        return response;
    }
}
