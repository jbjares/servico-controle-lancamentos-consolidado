# Implementação Base - Serviço de Controle de Lançamentos e Consolidado Diário

[![CI](https://github.com/jbjares/servico-controle-lancamentos-consolidado/actions/workflows/ci.yml/badge.svg)](https://github.com/jbjares/servico-controle-lancamentos-consolidado/actions/workflows/ci.yml)
[![K6 50 RPS Long](https://github.com/jbjares/servico-controle-lancamentos-consolidado/actions/workflows/k6-50rps-long.yml/badge.svg)](https://github.com/jbjares/servico-controle-lancamentos-consolidado/actions/workflows/k6-50rps-long.yml)

Esta entrega contém uma base executável e evolutiva com dois serviços Spring Boot:

- `servico-lancamentos` com duas replicas locais (`servico-lancamentos-a` e `servico-lancamentos-b`)
- `servico-consolidado-diario` com duas replicas locais (`servico-consolidado-diario-a` e `servico-consolidado-diario-b`)

Infraestrutura local:

- PostgreSQL
- RabbitMQ
- Nginx como load balancer/reverse proxy para as replicas dos dois servicos
- Prometheus e Grafana opcionais no profile `observability`
- Loki, Promtail, Alertmanager, cAdvisor, Node Exporter e Nginx Exporter opcionais no profile `full-ops`

## Pré-requisitos

- Docker Engine rodando no Ubuntu/WSL.
- Docker Compose no mesmo ambiente Linux/WSL.
- Java 11 e Maven apenas se quiser executar os serviços fora do Docker.
- Postman ou Insomnia para testes manuais por interface gráfica.

Entrar no projeto:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
```

## Modos de execução

A solução usa Docker Compose profiles para equilibrar simplicidade de POC e demonstração de arquitetura corporativa.

Use apenas um profile opcional por execução: `observability` para métricas/dashboards leves ou `full-ops` para a stack operacional completa.

### Core

Modo padrão recomendado para avaliação funcional. Sobe apenas o necessário para executar o negócio:

- PostgreSQL
- RabbitMQ
- duas replicas do `servico-lancamentos`
- duas replicas do `servico-consolidado-diario`
- Nginx como load balancer

Na raiz do projeto:

```bash
docker compose up --build
```

### Observability

Modo recomendado para demonstrar métricas, dashboards e SLO sem subir a stack operacional completa.

```bash
docker compose --profile observability up --build
```

Inclui o core mais:

- Prometheus
- Grafana

Os dashboards são provisionados automaticamente e continuam disponíveis nesse profile. Painéis que dependem de logs centralizados ou métricas de host/container ficam sem dados até o profile `full-ops` ser usado.

### Full Ops

Modo demonstrativo para evidenciar uma visão operacional mais completa, mas não recomendado como caminho padrão da POC.

```bash
docker compose --profile full-ops up --build
```

Inclui o core, Prometheus, Grafana e:

- Alertmanager
- Loki
- Promtail
- cAdvisor
- Node Exporter
- Nginx Exporter

## Serviços expostos por modo

### Core

- Load balancer / API unificada: http://localhost:8080
- Serviço de Lançamentos replica A: http://localhost:8081
- Serviço de Lançamentos replica B: http://localhost:8084
- Serviço de Consolidado Diário replica A: http://localhost:8082
- Serviço de Consolidado Diário replica B: http://localhost:8085
- RabbitMQ Management: http://localhost:15672
- RabbitMQ Prometheus metrics: http://localhost:15692/metrics
- PostgreSQL: localhost:5432

### Observability

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (`admin` / `admin`)

### Full Ops

- Alertmanager: http://localhost:9093
- Loki: http://localhost:3100
- Promtail metrics: http://localhost:9080/metrics
- cAdvisor: http://localhost:8083
- Node Exporter: http://localhost:9100/metrics
- Nginx Exporter: http://localhost:9113/metrics

## Rotas principais

- `POST http://localhost:8080/api/lancamentos`
- `GET http://localhost:8080/api/lancamentos?dataInicio=YYYY-MM-DD&dataFim=YYYY-MM-DD`
- `GET http://localhost:8080/api/consolidados/{data}`
- `GET http://localhost:8080/health`
- `GET http://localhost:8080/actuator/lancamentos/health/readiness`
- `GET http://localhost:8080/actuator/consolidado/health/readiness`

As portas `8081`, `8084`, `8082` e `8085` ficam expostas para diagnostico direto das replicas, mas os testes automatizados usam o load balancer em `8080`.

## Passo a passo rápido via linha de comando

1. Suba a stack recomendada para avaliação com observabilidade leve:

```bash
docker compose --profile observability up --build -d
```

2. Aguarde os endpoints ficarem prontos:

```bash
./tests/ci/wait-url.sh \
  http://localhost:8080/health \
  http://localhost:8081/actuator/health/readiness \
  http://localhost:8084/actuator/health/readiness \
  http://localhost:8082/actuator/health/readiness \
  http://localhost:8085/actuator/health/readiness \
  http://localhost:9090/-/ready \
  http://localhost:3000/api/health
```

3. Registre um crédito:

```bash
curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "CREDITO",
    "valor": 100.50,
    "dataEfetiva": "2026-04-21",
    "descricao": "Venda no caixa"
  }'
```

4. Registre um débito:

```bash
curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "DEBITO",
    "valor": 25.10,
    "dataEfetiva": "2026-04-21",
    "descricao": "Pagamento fornecedor"
  }'
```

5. Consulte os lançamentos:

```bash
curl -sS 'http://localhost:8080/api/lancamentos?dataInicio=2026-04-21&dataFim=2026-04-21'
```

6. Consulte o consolidado:

```bash
curl -i http://localhost:8080/api/consolidados/2026-04-21
```

O consolidado é atualizado de forma assíncrona. Se a primeira consulta retornar `404`, aguarde alguns segundos e consulte novamente.

## Passo a passo no Postman ou Insomnia

Crie uma environment:

| Variável | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `dataEfetiva` | `2026-04-21` |

Crie as requests:

| Nome | Método | URL |
|---|---|---|
| Health | `GET` | `{{baseUrl}}/health` |
| Criar crédito | `POST` | `{{baseUrl}}/api/lancamentos` |
| Criar débito | `POST` | `{{baseUrl}}/api/lancamentos` |
| Consultar lançamentos | `GET` | `{{baseUrl}}/api/lancamentos?dataInicio={{dataEfetiva}}&dataFim={{dataEfetiva}}` |
| Consultar consolidado | `GET` | `{{baseUrl}}/api/consolidados/{{dataEfetiva}}` |

Body para crédito:

```json
{
  "tipo": "CREDITO",
  "valor": 100.50,
  "dataEfetiva": "{{dataEfetiva}}",
  "descricao": "Venda via Postman"
}
```

Body para débito:

```json
{
  "tipo": "DEBITO",
  "valor": 25.10,
  "dataEfetiva": "{{dataEfetiva}}",
  "descricao": "Despesa via Postman"
}
```

Use sempre o header:

```text
Content-Type: application/json
```

Um roteiro completo com casos negativos, Maven, k6, JMeter e inspeção de banco está em [Execução e Testes](docs/execucao-testes.md).

## Observabilidade e saúde

- Lançamentos replica A readiness: http://localhost:8081/actuator/health/readiness
- Lançamentos replica B readiness: http://localhost:8084/actuator/health/readiness
- Lançamentos replica A Prometheus: http://localhost:8081/actuator/prometheus
- Lançamentos replica B Prometheus: http://localhost:8084/actuator/prometheus
- Consolidado replica A readiness: http://localhost:8082/actuator/health/readiness
- Consolidado replica B readiness: http://localhost:8085/actuator/health/readiness
- Consolidado replica A Prometheus: http://localhost:8082/actuator/prometheus
- Consolidado replica B Prometheus: http://localhost:8085/actuator/prometheus

Os healthchecks do Docker usam o endpoint de readiness. A readiness considera estado da aplicação, PostgreSQL e RabbitMQ.

## Logs, alertas e recursos

- Logs estruturados no console incluem `service`, `level`, `thread`, `logger` e mensagem.
- No profile `observability`, o Grafana é provisionado com dashboards baseados em métricas de aplicação, RabbitMQ, Outbox e SLO.
- No profile `full-ops`, Promtail coleta logs dos containers Docker e envia para o Loki.
- O Prometheus carrega regras para indisponibilidade, HTTP 5xx, backlog/erro do Outbox e filas RabbitMQ. No profile `full-ops`, também há cobertura para logs, memoria, CPU, disco e Nginx Exporter.
- O Alertmanager está configurado localmente com receiver `local-alerts`; integrações externas como e-mail, Slack, Teams ou PagerDuty podem ser adicionadas nesse receiver em uma evolução produtiva.

## Fluxo principal

1. O cliente registra um lançamento em `POST /api/lancamentos`
2. O serviço de lançamentos persiste o registro no schema `lancamentos`
3. O serviço grava o evento na tabela de Outbox na mesma transação
4. O publisher do Outbox publica o evento `LancamentoRegistrado` no RabbitMQ
5. O serviço de consolidado consome o evento
6. O consolidado diário é atualizado no schema `consolidado`
7. A consulta ocorre em `GET /api/consolidados/{data}`

## Mensageria e Outbox

- O `servico-lancamentos` grava o lançamento e o evento na tabela `lancamentos.outbox_evento` na mesma transação.
- Um publisher agendado reserva eventos `PENDENTE`/`ERRO` com status `PROCESSANDO`, lease e lock pessimista; depois publica no RabbitMQ, aguarda publisher confirm e marca como `PUBLICADO`.
- Em falha temporária do RabbitMQ, o evento permanece no banco, libera a reserva e recebe nova tentativa com backoff.
- A fila principal possui DLQ configurada em `lancamento.registrado.dlq`.
- O consumidor usa retry antes de rejeitar mensagens para DLQ.
- O serviço de lançamentos não depende do serviço de consolidado estar disponível.

Configurações principais do Outbox:

- `APP_OUTBOX_PUBLISHER_FIXED_DELAY_MS`: intervalo entre ciclos. Padrão: `500`.
- `APP_OUTBOX_PUBLISHER_BATCH_SIZE`: eventos por ciclo. Padrão: `100`.
- `APP_OUTBOX_PUBLISHER_CONFIRM_TIMEOUT_MS`: timeout do publisher confirm. Padrão: `5000`.
- `APP_OUTBOX_PUBLISHER_CLAIM_TIMEOUT_MS`: tempo máximo de reserva de um evento `PROCESSANDO`. Padrão: `30000`.

## CI

O workflow `.github/workflows/ci.yml` executa:

- testes Maven do `servico-lancamentos`
- testes Maven do `servico-consolidado-diario`
- validação e subida do `docker-compose.yml`
- espera de readiness das quatro replicas e load balancer
- smoke do profile `full-ops` para garantir que a observabilidade completa continua válida
- k6 smoke test contra `POST /api/lancamentos` via load balancer
- teste de resiliência com `servico-consolidado-diario` parado
- k6 curto de 50 req/s por 10 segundos para PR/push/manual

O workflow `.github/workflows/k6-50rps-long.yml` roda o k6 50 req/s longo de forma manual e também em agenda nightly.

O Grafana provisiona os dashboards `Controle Lancamentos - Spring Boot`, `Controle Lancamentos - Operacao` e `Controle Lancamentos - SLO e Mensageria`.

## Testes de carga, stress e performance

O script k6 fica em `tests/load/k6/lancamentos-50rps.js` e valida 50 req/s com perda máxima padrão de 5%.

```bash
docker run --rm --network host \
  -e BASE_URL=http://localhost:8080 \
  -e RATE=50 \
  -e DURATION=1m \
  -e MAX_LOSS_RATE=0.05 \
  -e DATA_EFETIVA=$(date +%F) \
  -e TEST_RUN_ID=k6-local-$(date +%Y%m%d%H%M%S) \
  -v "$PWD/tests/load/k6:/scripts:ro" \
  grafana/k6 run /scripts/lancamentos-50rps.js
```

Também há um plano JMeter opcional em `tests/load/jmeter/lancamentos-carga-stress-performance.jmx`, útil para times que preferem evidências em `.jtl` e relatórios de performance tradicionais.

```bash
mkdir -p tests/load/results

docker run --rm --network host \
  -v "$PWD/tests/load/jmeter:/plans:ro" \
  -v "$PWD/tests/load/results:/results" \
  justb4/jmeter:5.6.3 \
  -n \
  -t /plans/lancamentos-carga-stress-performance.jmx \
  -l /results/lancamentos.jtl \
  -JHOST=localhost \
  -JPORT=8080 \
  -JTHREADS=80 \
  -JRAMP_UP=10 \
  -JDURATION=60 \
  -JDATA_EFETIVA=2099-01-05 \
  -JTEST_RUN_ID=jmeter-local-$(date +%Y%m%d%H%M%S)
```

## Segurança

A proteção de autenticação e autorização dos endpoints está documentada como arquitetura alvo e débito técnico da próxima sprint.

O desenho recomendado usa OAuth2/OIDC com token JWT do tipo Bearer, emitido pelo Keycloak a partir do Realm da aplicação. O Keycloak é a ferramenta de IAM/OAuth2/OIDC, e o Realm é o domínio lógico onde serão cadastrados usuários, clients, roles, groups, scopes e demais metadados de autenticação/autorização. Os serviços Spring Boot devem atuar como Resource Servers, validando assinatura por JWKS, `issuer`, `audience`, expiração e escopos antes de autorizar operações como:

- `lancamentos:write` para `POST /api/lancamentos`;
- `lancamentos:read` para `GET /api/lancamentos`;
- `consolidados:read` para `GET /api/consolidados/{data}`.

Nesta POC, essa validação ainda não está ativa nos endpoints para preservar simplicidade de execução local. A decisão e o plano de evolução estão detalhados em [Segurança](docs/seguranca.md), [Arquitetura](docs/arquitetura.md) e [Backlog técnico](docs/backlog-tecnico.md).

## Estrutura

```text
servico-controle-lancamentos-consolidado/
├── README.md
├── docker-compose.yml
├── docs/
├── docker/
├── servico-lancamentos/
├── servico-consolidado-diario/
└── tests/
```

## Documentação

- [Arquitetura](docs/arquitetura.md)
- [Execução e Testes](docs/execucao-testes.md)
- [Trade-offs](docs/tradeoffs.md)
- [Backlog técnico](docs/backlog-tecnico.md)
- [Modelo de domínio](docs/modelo-dominio.md)
- [Modelo de dados](docs/modelo-dados.md)
- [Segurança](docs/seguranca.md)
- [Roteiro de apresentação](docs/roteiro-apresentacao.md)

## Observações

- A solução adota consistência eventual
- O consolidado possui proteção simples de idempotência por `idEvento`
- Cada serviço versiona seu schema com Liquibase
- Esta base prioriza clareza, compilação e evolução gradual
