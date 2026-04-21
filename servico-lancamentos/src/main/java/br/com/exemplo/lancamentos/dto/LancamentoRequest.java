package br.com.exemplo.lancamentos.dto;

import br.com.exemplo.lancamentos.model.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LancamentoRequest {
    private TipoLancamento tipo;
    private BigDecimal valor;
    private LocalDate dataEfetiva;
    private String descricao;

    public TipoLancamento getTipo() { return tipo; }
    public void setTipo(TipoLancamento tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
