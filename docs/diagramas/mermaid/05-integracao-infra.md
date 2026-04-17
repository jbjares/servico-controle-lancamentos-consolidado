```mermaid
flowchart TB
    subgraph Aplicacoes
        L[Serviço de Lançamentos]
        C[Serviço de Consolidado Diário]
    end

    subgraph Infraestrutura
        MQ[RabbitMQ]
        PG[(PostgreSQL)]
    end

    L --> MQ
    MQ --> C
    L --> PG
    C --> PG
```