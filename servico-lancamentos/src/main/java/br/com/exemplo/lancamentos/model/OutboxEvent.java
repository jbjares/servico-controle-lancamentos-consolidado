package br.com.exemplo.lancamentos.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEvent {

    @Id
    @Column(name = "id_evento", nullable = false)
    private UUID idEvento;

    @Column(name = "tipo_evento", nullable = false, length = 120)
    private String tipoEvento;

    @Column(name = "correlation_id", nullable = false, length = 120)
    private String correlationId;

    @Column(name = "exchange_name", nullable = false, length = 120)
    private String exchangeName;

    @Column(name = "routing_key", nullable = false, length = 120)
    private String routingKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private OffsetDateTime proximaTentativaEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "publicado_em")
    private OffsetDateTime publicadoEm;

    @Column(name = "processando_por", length = 120)
    private String processandoPor;

    @Column(name = "processando_em")
    private OffsetDateTime processandoEm;

    @Column(name = "claim_expira_em")
    private OffsetDateTime claimExpiraEm;

    @Column(name = "ultimo_erro", length = 1000)
    private String ultimoErro;

    public UUID getIdEvento() { return idEvento; }
    public void setIdEvento(UUID idEvento) { this.idEvento = idEvento; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }

    public Integer getTentativas() { return tentativas; }
    public void setTentativas(Integer tentativas) { this.tentativas = tentativas; }

    public OffsetDateTime getProximaTentativaEm() { return proximaTentativaEm; }
    public void setProximaTentativaEm(OffsetDateTime proximaTentativaEm) { this.proximaTentativaEm = proximaTentativaEm; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(OffsetDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public OffsetDateTime getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(OffsetDateTime publicadoEm) { this.publicadoEm = publicadoEm; }

    public String getProcessandoPor() { return processandoPor; }
    public void setProcessandoPor(String processandoPor) { this.processandoPor = processandoPor; }

    public OffsetDateTime getProcessandoEm() { return processandoEm; }
    public void setProcessandoEm(OffsetDateTime processandoEm) { this.processandoEm = processandoEm; }

    public OffsetDateTime getClaimExpiraEm() { return claimExpiraEm; }
    public void setClaimExpiraEm(OffsetDateTime claimExpiraEm) { this.claimExpiraEm = claimExpiraEm; }

    public String getUltimoErro() { return ultimoErro; }
    public void setUltimoErro(String ultimoErro) { this.ultimoErro = ultimoErro; }
}
