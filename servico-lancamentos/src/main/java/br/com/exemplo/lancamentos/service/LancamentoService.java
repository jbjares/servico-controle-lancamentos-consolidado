package br.com.exemplo.lancamentos.service;

import br.com.exemplo.lancamentos.dto.LancamentoRequest;
import br.com.exemplo.lancamentos.dto.LancamentoResponse;
import br.com.exemplo.lancamentos.event.LancamentoRegistradoEvent;
import br.com.exemplo.lancamentos.model.Lancamento;
import br.com.exemplo.lancamentos.repository.LancamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LancamentoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoService.class);

    private final LancamentoRepository repository;
    private final OutboxService outboxService;
    private final String exchange;
    private final String routingKey;

    public LancamentoService(LancamentoRepository repository,
                             OutboxService outboxService,
                             @Value("${app.mensageria.exchange}") String exchange,
                             @Value("${app.mensageria.routing-key}") String routingKey) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Transactional
    public LancamentoResponse registrar(LancamentoRequest request) {
        validar(request);

        Lancamento lancamento = new Lancamento();
        lancamento.setId(UUID.randomUUID());
        lancamento.setTipo(request.getTipo());
        lancamento.setValor(request.getValor());
        lancamento.setDataEfetiva(request.getDataEfetiva());
        lancamento.setDescricao(request.getDescricao());
        lancamento.setCriadoEm(OffsetDateTime.now());

        repository.save(lancamento);

        LancamentoRegistradoEvent event = new LancamentoRegistradoEvent();
        event.setIdEvento(UUID.randomUUID());
        event.setCorrelationId("corr-" + lancamento.getId());
        event.setIdLancamento(lancamento.getId());
        event.setTipo(lancamento.getTipo());
        event.setValor(lancamento.getValor());
        event.setDataEfetiva(lancamento.getDataEfetiva());
        event.setDescricao(lancamento.getDescricao());
        event.setOcorridoEm(OffsetDateTime.now());
        event.setVersaoEvento(1);

        outboxService.registrarLancamentoRegistrado(event, exchange, routingKey);
        LOGGER.info("Lancamento recebido idLancamento={} idEvento={} tipo={} valor={} dataEfetiva={}",
                lancamento.getId(), event.getIdEvento(), lancamento.getTipo(), lancamento.getValor(), lancamento.getDataEfetiva());

        return new LancamentoResponse(lancamento.getId(), "RECEBIDO", "Lancamento registrado com sucesso");
    }

    public List<Lancamento> consultar(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) {
            return repository.findAll();
        }
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Informe dataInicio e dataFim juntos, ou omita ambos para listar todos os lancamentos");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim deve ser maior ou igual a dataInicio");
        }
        return repository.findByDataEfetivaBetween(dataInicio, dataFim);
    }

    private void validar(LancamentoRequest request) {
        if (request.getTipo() == null) {
            throw new IllegalArgumentException("O tipo do lancamento e obrigatorio");
        }
        if (request.getValor() == null || request.getValor().signum() <= 0) {
            throw new IllegalArgumentException("O valor do lancamento deve ser maior que zero");
        }
        if (request.getDataEfetiva() == null) {
            throw new IllegalArgumentException("A data efetiva e obrigatoria");
        }
    }
}