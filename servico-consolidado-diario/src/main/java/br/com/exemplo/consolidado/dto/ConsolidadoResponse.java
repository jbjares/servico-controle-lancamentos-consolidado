package br.com.exemplo.consolidado.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ConsolidadoResponse {
    private LocalDate dataEfetiva;
    private BigDecimal totalCreditos;
    private BigDecimal totalDebitos;
    private BigDecimal saldoFinal;
    private OffsetDateTime ultimaAtualizacao;

    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }

    public BigDecimal getTotalCreditos() { return totalCreditos; }
    public void setTotalCreditos(BigDecimal totalCreditos) { this.totalCreditos = totalCreditos; }

    public BigDecimal getTotalDebitos() { return totalDebitos; }
    public void setTotalDebitos(BigDecimal totalDebitos) { this.totalDebitos = totalDebitos; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public OffsetDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(OffsetDateTime ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }
}
