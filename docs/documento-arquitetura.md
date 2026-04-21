# Documento de Arquitetura

Este arquivo existe como ponto de entrada com nome explícito para avaliação arquitetural do desafio.

O documento arquitetural completo e canônico está em [`arquitetura.md`](arquitetura.md). Ele foi revisado para refletir o estado atual da solução, incluindo:

- múltiplas réplicas dos serviços;
- Nginx como load balancer;
- Docker Compose profiles `core`, `observability` e `full-ops`;
- Outbox Pattern implementado no `servico-lancamentos`;
- publisher com status `PROCESSANDO`, lease e publisher confirm;
- RabbitMQ com retry e DLQ;
- idempotência no `servico-consolidado-diario` via `evento_processado`;
- métricas com Actuator/Prometheus;
- dashboards Grafana;
- testes unitários, integração, k6 e JMeter;
- estratégia de segurança e evolução para OAuth2/OIDC.

## Leitura Recomendada

| Documento | Finalidade |
|---|---|
| [`arquitetura.md`](arquitetura.md) | Documento arquitetural completo |
| [`modelo-dominio.md`](modelo-dominio.md) | Entidades, eventos e regras de domínio |
| [`modelo-dados.md`](modelo-dados.md) | Schemas, tabelas, colunas e consultas úteis |
| [`tradeoffs.md`](tradeoffs.md) | Decisões arquiteturais e alternativas consideradas |
| [`seguranca.md`](seguranca.md) | Estratégia de segurança para POC e produção |
| [`execucao-testes.md`](execucao-testes.md) | Execução local, Postman, Insomnia, Maven, k6 e JMeter |
| [`backlog-tecnico.md`](backlog-tecnico.md) | Itens implementados, próximos passos e evolução produtiva |

## Resumo da Arquitetura Atual

A solução é composta por dois serviços Spring Boot:

- `servico-lancamentos`
- `servico-consolidado-diario`

O `servico-lancamentos` recebe lançamentos de crédito e débito, persiste no schema `lancamentos` e registra o evento correspondente na tabela `outbox_evento` dentro da mesma transação. O publisher do Outbox publica o evento no RabbitMQ de forma assíncrona.

O `servico-consolidado-diario` consome os eventos de lançamentos, atualiza o schema `consolidado` e registra cada evento processado para garantir idempotência.

Esse desenho atende ao requisito não funcional principal: o serviço de controle de lançamentos permanece disponível mesmo quando o consolidado diário está indisponível.

## Modos de Execução

```bash
docker compose up --build -d
```

Sobe o core funcional da POC.

```bash
docker compose --profile observability up --build -d
```

Sobe o core mais Prometheus e Grafana.

```bash
docker compose --profile full-ops up --build -d
```

Sobe a stack operacional completa com Loki, Promtail, Alertmanager e exporters.

## Passo a Passo Docker Completo

Esta seção descreve como inicializar todos os módulos, serviços e ferramentas da solução via Docker Compose.

### 1. Entrar no diretório do projeto

No Ubuntu/WSL:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
```

### 2. Validar o arquivo Docker Compose

Antes de subir os containers, valide a configuração completa:

```bash
docker compose --profile full-ops config
```

Esse comando valida o compose com todos os módulos e ferramentas operacionais.

### 3. Subir todos os módulos e ferramentas

Para inicializar a stack completa:

```bash
docker compose --profile full-ops up --build -d
```

Esse modo sobe:

| Componente | Finalidade |
|---|---|
| `postgres` | Banco PostgreSQL da solução |
| `rabbitmq` | Broker de mensageria, management UI e métricas Prometheus |
| `servico-lancamentos-a` | Réplica A do serviço de lançamentos |
| `servico-lancamentos-b` | Réplica B do serviço de lançamentos |
| `servico-consolidado-diario-a` | Réplica A do serviço de consolidado diário |
| `servico-consolidado-diario-b` | Réplica B do serviço de consolidado diário |
| `nginx` | Load balancer e ponto único de entrada da API |
| `prometheus-full-ops` | Coleta de métricas da aplicação, RabbitMQ, Nginx e infraestrutura |
| `grafana-full-ops` | Dashboards visuais |
| `alertmanager` | Gerenciamento local de alertas |
| `loki` | Consolidação de logs |
| `promtail` | Coleta de logs dos containers Docker |
| `cadvisor` | Métricas de containers |
| `node-exporter` | Métricas do host/WSL |
| `nginx-exporter` | Métricas do Nginx |

### 4. Aguardar readiness dos serviços

Execute:

```bash
./tests/ci/wait-url.sh \
  http://localhost:8080/health \
  http://localhost:8081/actuator/health/readiness \
  http://localhost:8084/actuator/health/readiness \
  http://localhost:8082/actuator/health/readiness \
  http://localhost:8085/actuator/health/readiness \
  http://localhost:9090/-/ready \
  http://localhost:9093/-/ready \
  http://localhost:3000/api/health \
  http://localhost:3100/ready \
  http://localhost:9080/metrics \
  http://localhost:9113/metrics \
  http://localhost:8083/metrics \
  http://localhost:9100/metrics \
  http://localhost:15692/metrics
```

### 5. Verificar containers ativos

```bash
docker compose --profile full-ops ps
```

### 6. Acessar URLs principais

| Recurso | URL | Credenciais |
|---|---|---|
| API via Nginx | `http://localhost:8080` | - |
| Health do load balancer | `http://localhost:8080/health` | - |
| Lançamentos réplica A | `http://localhost:8081` | - |
| Lançamentos réplica B | `http://localhost:8084` | - |
| Consolidado réplica A | `http://localhost:8082` | - |
| Consolidado réplica B | `http://localhost:8085` | - |
| RabbitMQ Management | `http://localhost:15672` | `guest` / `guest` |
| RabbitMQ Metrics | `http://localhost:15692/metrics` | - |
| Prometheus | `http://localhost:9090` | - |
| Alertmanager | `http://localhost:9093` | - |
| Grafana | `http://localhost:3000` | `admin` / `admin` |
| Loki | `http://localhost:3100` | - |
| Promtail Metrics | `http://localhost:9080/metrics` | - |
| Nginx Exporter | `http://localhost:9113/metrics` | - |
| cAdvisor | `http://localhost:8083` | - |
| Node Exporter | `http://localhost:9100/metrics` | - |
| PostgreSQL | `localhost:5432` | `app` / `app` |

### 7. Validar fluxo funcional

Crie um lançamento:

```bash
curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "CREDITO",
    "valor": 100.50,
    "dataEfetiva": "2026-04-21",
    "descricao": "Teste full-ops"
  }'
```

Consulte os lançamentos do dia:

```bash
curl -sS 'http://localhost:8080/api/lancamentos?dataInicio=2026-04-21&dataFim=2026-04-21'
```

Consulte o consolidado:

```bash
curl -i http://localhost:8080/api/consolidados/2026-04-21
```

Como a consolidação é assíncrona, uma primeira consulta pode retornar `404`. Aguarde alguns segundos e consulte novamente.

### 8. Validar filas RabbitMQ

```bash
docker exec rabbitmq-controle-financeiro rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Resultado esperado em operação normal:

- fila principal sem backlog persistente;
- DLQ vazia;
- consumidores ativos na fila principal.

### 9. Validar Prometheus e dashboards

Prometheus targets:

```bash
curl -sS 'http://localhost:9090/api/v1/targets?state=active'
```

Grafana:

```bash
curl -sS -u admin:admin 'http://localhost:3000/api/search?query=Controle%20Lancamentos'
```

Dashboards esperados:

- `Controle Lancamentos`
- `Controle Lancamentos - Operacao`
- `Controle Lancamentos - SLO e Mensageria`
- `Controle Lancamentos - Spring Boot`

### 10. Encerrar todos os módulos e ferramentas

Para parar a stack completa:

```bash
docker compose --profile full-ops down --remove-orphans
```

Para parar e remover também volumes locais:

```bash
docker compose --profile full-ops down -v --remove-orphans
```

Use `-v` apenas quando desejar apagar dados locais de banco, métricas e dashboards.

## Observação Sobre Duplicidade

Para evitar divergência entre documentos, este arquivo não replica todo o conteúdo de [`arquitetura.md`](arquitetura.md). Ele funciona como capa, índice e resumo executivo do documento arquitetural principal.
