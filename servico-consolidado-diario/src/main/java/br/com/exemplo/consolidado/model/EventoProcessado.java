package br.com.exemplo.consolidado.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_processado")
public class EventoProcessado {

    @Id
    @Column(name = "id_evento", nullable = false)
    private UUID idEvento;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "processado_em", nullable = false)
    private OffsetDateTime processadoEm;

    public UUID getIdEvento() { return idEvento; }
    public void setIdEvento(UUID idEvento) { this.idEvento = idEvento; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public OffsetDateTime getProcessadoEm() { return processadoEm; }
    public void setProcessadoEm(OffsetDateTime processadoEm) { this.processadoEm = processadoEm; }
}
