```mermaid
flowchart TB
    subgraph ServicoLancamentos
        LC[Controller]
        LS[Service]
        LR[Repository]
        LP[PublicadorEvento]
    end

    subgraph ServicoConsolidado
        CC[Consumer]
        CS[Service]
        CR[Repository]
        CE[RepositorioEventoProcessado]
    end

    LC --> LS
    LS --> LR
    LS --> LP
    CC --> CS
    CS --> CR
    CS --> CE
```