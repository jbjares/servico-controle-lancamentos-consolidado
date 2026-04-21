package br.com.exemplo.consolidado.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "consolidado_diario")
public class ConsolidadoDiario {

    @Id
    @Column(name = "data_efetiva", nullable = false)
    private LocalDate dataEfetiva;

    @Column(name = "total_creditos", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCreditos;

    @Column(name = "total_debitos", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebitos;

    @Column(name = "saldo_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "ultima_atualizacao", nullable = false)
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
