# Trade-offs

Este documento registra as principais decisões arquiteturais, alternativas consideradas e justificativas.

| Decisão | Alternativa | Benefício | Custo/Complexidade | Decisão adotada | Justificativa |
|---|---|---|---|---|---|
| Dois serviços Spring Boot | Monólito modular | Isolamento de falhas e escala independente | Mais componentes para operar | Serviços separados | O RNF exige que lançamentos continue disponível mesmo se consolidado cair |
| Integração assíncrona | Chamada HTTP síncrona entre serviços | Desacoplamento e absorção de falhas temporárias | Consistência eventual | RabbitMQ | Atende a disponibilidade do serviço de lançamentos |
| Outbox Pattern | Publicar direto no RabbitMQ dentro do fluxo HTTP | Garante que lançamento salvo gere evento publicável | Mais tabela, publisher e controle de status | Outbox implementado | Evita perda de evento em falha temporária do RabbitMQ |
| RabbitMQ | Kafka | Menor complexidade e boa aderência a comandos/eventos discretos | Menor vocação para streaming massivo | RabbitMQ | Melhor custo-benefício para POC e requisito de 50 req/s |
| Banco único com schemas | Banco por serviço | Simplicidade local e menor custo operacional | Menor isolamento físico | PostgreSQL único com schemas `lancamentos` e `consolidado` | Preserva separação lógica com baixa fricção para avaliação |
| Liquibase embarcado | Migrations em projeto separado | Autonomia de cada serviço e startup simples | Migração roda no ciclo de vida da aplicação | Liquibase por serviço | Facilita execução local e versionamento claro |
| Duas réplicas locais por serviço | Uma réplica por serviço | Demonstra escala horizontal e balanceamento | Mais containers | Duas réplicas de cada serviço | Evidencia disponibilidade e distribuição pelo Nginx |
| Nginx local | Acesso direto às portas das réplicas | Ponto único de entrada e load balancing | Mais um container | Nginx no core | Aproxima o desenho de uma borda real sem complexidade alta |
| Docker Compose profiles | Subir toda a infra sempre | Permite POC enxuta e operação completa opcional | Mais documentação | `core`, `observability`, `full-ops` | Evita overengineering no caminho padrão |
| k6 no CI | JMeter como teste principal | Execução leve, scriptável e adequada a CI | Menos familiar para alguns times QA | k6 automatizado | Valida 50 req/s e perda máxima de 5% de forma objetiva |
| JMeter opcional | Apenas k6 | Evidência tradicional de performance | Mais ferramenta para manter | JMeter em `tests/load/jmeter` | Útil para execução local e análise `.jtl`, sem pesar no CI |
| Segurança por evolução incremental | Keycloak/OIDC completo no compose | POC simples e avaliável | OAuth2 real fica documentado como arquitetura alvo | JWT Bearer com Keycloak como próxima sprint, rate limit na borda | Evita transformar a POC em plataforma de IAM |

## Observabilidade

Foram definidos três níveis:

- `core`: execução funcional mínima.
- `observability`: Prometheus e Grafana para métricas, Outbox, RabbitMQ e SLO.
- `full-ops`: Loki, Promtail, Alertmanager e exporters de infraestrutura.

Essa divisão demonstra maturidade operacional sem obrigar o avaliador a subir componentes acessórios para validar o fluxo de negócio.

## Consistência Eventual

O endpoint `POST /api/lancamentos` retorna após persistir o lançamento e registrar o evento no Outbox. O consolidado diário é atualizado de forma assíncrona pelo consumidor. Por isso, uma consulta imediata a `GET /api/consolidados/{data}` pode retornar `404` por alguns instantes até o evento ser processado.

## Segurança

OAuth2/OIDC com JWT Bearer é a arquitetura alvo recomendada para produção. A POC documenta essa decisão como débito técnico da próxima sprint, mantendo a execução local enxuta.

Na evolução planejada:

- headers de segurança e rate limiting no Nginx;
- Spring Security OAuth2 Resource Server nos dois serviços;
- tokens emitidos pelo Keycloak a partir do Realm da aplicação;
- Realm concentrando usuários, clients, roles, groups, scopes e metadados de autenticação/autorização;
- validação de assinatura via JWKS, issuer, audience, expiração e escopos;
- Actuator restrito ao mínimo necessário;
- secrets fora do código em ambientes reais.

Detalhes estão em [Segurança](seguranca.md).
