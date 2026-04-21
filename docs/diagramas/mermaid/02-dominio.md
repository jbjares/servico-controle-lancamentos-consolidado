```mermaid
classDiagram
    class Lancamento {
        +UUID id
        +TipoLancamento tipo
        +Decimal valor
        +Date dataEfetiva
        +String descricao
        +Timestamp criadoEm
    }

    class ConsolidadoDiario {
        +Date dataEfetiva
        +Decimal totalCreditos
        +Decimal totalDebitos
        +Decimal saldoFinal
        +Timestamp ultimaAtualizacao
    }

    class OutboxEvent {
        +UUID idEvento
        +String tipoEvento
        +String correlationId
        +String exchangeName
        +String routingKey
        +String payload
        +OutboxStatus status
        +Integer tentativas
        +Timestamp proximaTentativaEm
        +Timestamp publicadoEm
        +String processandoPor
        +Timestamp claimExpiraEm
    }

    class EventoProcessado {
        +UUID idEvento
        +String correlationId
        +Timestamp processadoEm
    }

    Lancamento --> OutboxEvent : gera
    OutboxEvent --> ConsolidadoDiario : evento publicado atualiza
    ConsolidadoDiario --> EventoProcessado : registra idempotencia
```
