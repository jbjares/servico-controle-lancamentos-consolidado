## A solução foi organizada para demonstrar, de forma objetiva, os principais fundamentos de arquitetura:
## tomada de decisão, decomposição, análise de impacto, trade-offs, priorização, visão sistêmica e comunicação técnica.
## O MVP cobre os fluxos essenciais, enquanto os itens produtivos complementares foram tratados como backlog evolutivo documentado.


---

| Eixo                    | Implementado                                | Evidência                      |
| ----------------------- | ------------------------------------------- | ------------------------------ |
| Controle de lançamentos | API para registrar crédito/débito           | `curl`, banco `lancamento`     |
| Mensageria              | Publicação assíncrona via RabbitMQ          | Exchange, fila, routing key    |
| Outbox                  | Evento persistido antes do envio            | tabela `outbox_evento`         |
| Consolidação diária     | Atualização por `data_efetiva`              | tabela `consolidado_diario`    |
| Idempotência            | Controle de evento já processado            | tabela `evento_processado`     |
| Resiliência básica      | Consolidado pode cair sem perder lançamento | teste “consolidado down”       |
| CI/CD                   | Testes Maven, Docker Compose e k6           | GitHub Actions                 |
| RNF                     | Teste de carga curto com k6                 | `50 req/s`                     |
| Observabilidade mínima  | CorrelationId nos eventos                   | rastreabilidade entre serviços |



| Item                     | Objetivo                                            | Prioridade  |
| ------------------------ | --------------------------------------------------- | ----------- |
| DLQ                      | Isolar mensagens com erro definitivo                | Alta        |
| Retry com backoff        | Reprocessar falhas transitórias                     | Alta        |
| Outbox Scheduler robusto | Garantir publicação confiável dos eventos pendentes | Alta        |
| Observabilidade          | Métricas, logs estruturados e tracing               | Média       |
| Segurança OAuth2/JWT     | Proteger APIs                                       | Média       |
| API Gateway              | Centralizar entrada, autenticação e roteamento      | Média       |
| Redis                    | Cache ou otimização de leitura                      | Baixa/Média |
| Kafka                    | Evolução para alto volume/event streaming           | Baixa       |
| Kubernetes/OpenShift     | Orquestração produtiva                              | Baixa/Média |
| Testes E2E               | Validar fluxo completo ponta a ponta                | Média       |



| Tema                     | Decisão tomada                          | Trade-off                                                  |
| ------------------------ | --------------------------------------- | ---------------------------------------------------------- |
| Serviços separados       | Lançamentos e consolidado independentes | Mais complexidade que monólito, porém maior desacoplamento |
| RabbitMQ                 | Mensageria simples e robusta para POC   | Menos adequado que Kafka para event streaming massivo      |
| Outbox                   | Reduz risco de perder evento            | Exige tabela e processo de publicação                      |
| Idempotência             | Evita duplicidade no consolidado        | Exige controle de eventos processados                      |
| Consolidação incremental | Atualiza saldo próximo do tempo real    | Mais complexa que batch noturno                            |
| CI/CD com testes         | Evidencia qualidade e repetibilidade    | Aumenta esforço inicial                                    |
| DLQ como backlog         | Reconhece evolução produtiva necessária | Não foi implementado no MVP                                |

