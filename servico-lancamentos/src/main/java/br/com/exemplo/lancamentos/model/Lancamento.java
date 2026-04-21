package br.com.exemplo.lancamentos.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lancamento")
public class Lancamento {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLancamento tipo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_efetiva", nullable = false)
    private LocalDate dataEfetiva;

    @Column(length = 255)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TipoLancamento getTipo() { return tipo; }
    public void setTipo(TipoLancamento tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
