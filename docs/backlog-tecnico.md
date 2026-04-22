# Backlog Técnico

Este backlog separa o que já foi implementado, o que é recomendado como próximo passo e o que faz mais sentido em uma evolução produtiva.

## Implementado

- Dois serviços Spring Boot 2.7.x.
- PostgreSQL com schemas `lancamentos` e `consolidado`.
- Liquibase por serviço.
- RabbitMQ com exchange, fila principal e DLQ.
- Outbox Pattern no `servico-lancamentos`.
- Publisher do Outbox com reserva `PROCESSANDO`, lease e publisher confirm.
- Retry no produtor/consumidor RabbitMQ.
- Idempotência no consolidado via tabela `evento_processado`.
- Nginx como load balancer para múltiplas réplicas.
- Docker Compose profiles `core`, `observability` e `full-ops`.
- Actuator com readiness/liveness.
- Prometheus e Grafana no profile `observability`.
- Loki, Promtail, Alertmanager e exporters no profile `full-ops`.
- Testes unitários e de integração com H2.
- k6 para validação de 50 req/s e perda máxima de 5%.
- JMeter opcional para carga, stress e performance.
- CI com Maven tests, compose smoke, resiliência e k6.
- Arquitetura alvo de segurança documentada com JWT Bearer, Keycloak, roles e scopes.

## Próximos passos recomendados

1. Implementar autenticação/autorização com Spring Security OAuth2 Resource Server.
2. Criar o Realm da aplicação no Keycloak com users, clients, roles, groups, scopes e metadados de autenticação/autorização.
3. Adicionar profile opcional `security` no Docker Compose com Keycloak.
4. Adicionar rate limiting e headers de segurança no Nginx.
5. Criar uma UI web simples para registrar lançamentos e consultar consolidado diário.
6. Adicionar coleção Postman/Insomnia versionada com obtenção de token Bearer.
7. Adicionar teste JMeter em workflow manual, se o tempo de execução for aceitável.
8. Evoluir dashboards com painéis específicos por rota e percentis p95/p99.
9. Criar alertas de Outbox com thresholds ajustáveis por ambiente.

## Evoluções produtivas

- OAuth2/OIDC com JWT Bearer, integrado a Keycloak, Azure AD, Okta ou IdP corporativo.
- TLS externo e, se necessário, TLS interno entre serviços.
- Secret manager para senhas, tokens e chaves.
- Banco por serviço ou instância segregada, se houver exigência forte de isolamento.
- Migração para Kubernetes com HPA, readiness/liveness e Ingress Controller.
- Observabilidade com OpenTelemetry e tracing distribuído.
- Políticas de backup/restore testadas para PostgreSQL.
- Retenção e mascaramento de logs.
- DLQ com rotina operacional de inspeção, replay e descarte controlado.

## Itens deliberadamente fora do core

- Keycloak local por padrão no core da POC.
- Kafka.
- Redis/cache.
- Service mesh.
- Kubernetes local.
- Alertas externos reais por e-mail/Slack/PagerDuty.

Esses itens são tecnicamente válidos, mas aumentariam a complexidade da POC sem serem necessários para demonstrar o requisito central do desafio.
