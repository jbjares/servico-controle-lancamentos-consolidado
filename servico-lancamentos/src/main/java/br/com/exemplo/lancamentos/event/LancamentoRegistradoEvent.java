package br.com.exemplo.lancamentos.event;

import br.com.exemplo.lancamentos.model.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class LancamentoRegistradoEvent {
    private UUID idEvento;
    private String correlationId;
    private UUID idLancamento;
    private TipoLancamento tipo;
    private BigDecimal valor;
    private LocalDate dataEfetiva;
    private String descricao;
    private OffsetDateTime ocorridoEm;
    private Integer versaoEvento;

    public UUID getIdEvento() { return idEvento; }
    public void setIdEvento(UUID idEvento) { this.idEvento = idEvento; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public UUID getIdLancamento() { return idLancamento; }
    public void setIdLancamento(UUID idLancamento) { this.idLancamento = idLancamento; }

    public TipoLancamento getTipo() { return tipo; }
    public void setTipo(TipoLancamento tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(OffsetDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }

    public Integer getVersaoEvento() { return versaoEvento; }
    public void setVersaoEvento(Integer versaoEvento) { this.versaoEvento = versaoEvento; }
}
