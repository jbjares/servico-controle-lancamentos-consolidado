# Execução e Testes

Este guia descreve como executar e testar a solução por linha de comando, Postman e Insomnia.

## Pré-requisitos

- Docker Engine rodando no Ubuntu/WSL.
- Docker Compose disponível no mesmo ambiente WSL.
- Java 11 e Maven, caso queira rodar os serviços fora do Docker.
- Postman ou Insomnia, caso prefira testar por interface gráfica.

Todos os comandos abaixo assumem execução na raiz do projeto:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
```

## Subir a solução

### Core

Sobe a POC funcional mínima:

```bash
docker compose up --build -d
```

Serviços incluídos:

- PostgreSQL
- RabbitMQ
- Nginx
- `servico-lancamentos-a`
- `servico-lancamentos-b`
- `servico-consolidado-diario-a`
- `servico-consolidado-diario-b`

### Observability

Sobe o core mais Prometheus e Grafana:

```bash
docker compose --profile observability up --build -d
```

### Full Ops

Sobe a stack operacional completa:

```bash
docker compose --profile full-ops up --build -d
```

### Parar a solução

Para parar apenas o modo padrão:

```bash
docker compose down
```

Para parar a stack com profiles e remover órfãos:

```bash
docker compose --profile full-ops down --remove-orphans
```

Para remover volumes também:

```bash
docker compose --profile full-ops down -v --remove-orphans
```

Use `-v` apenas quando quiser limpar banco, métricas e dados locais.

## Validar saúde

```bash
./tests/ci/wait-url.sh \
  http://localhost:8080/health \
  http://localhost:8081/actuator/health/readiness \
  http://localhost:8084/actuator/health/readiness \
  http://localhost:8082/actuator/health/readiness \
  http://localhost:8085/actuator/health/readiness
```

Com observabilidade:

```bash
./tests/ci/wait-url.sh \
  http://localhost:9090/-/ready \
  http://localhost:3000/api/health
```

## URLs principais

| Recurso | URL |
|---|---|
| API via Nginx | `http://localhost:8080` |
| Health do Nginx | `http://localhost:8080/health` |
| Lançamentos A | `http://localhost:8081` |
| Lançamentos B | `http://localhost:8084` |
| Consolidado A | `http://localhost:8082` |
| Consolidado B | `http://localhost:8085` |
| RabbitMQ Management | `http://localhost:15672` |
| RabbitMQ Metrics | `http://localhost:15692/metrics` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

Credenciais locais:

- RabbitMQ: `guest` / `guest`
- Grafana: `admin` / `admin`
- PostgreSQL: database `controle_financeiro`, usuário `app`, senha `app`

## Autenticação e autorização

Nesta POC, os comandos de teste abaixo ainda não exigem token Bearer. A autenticação/autorização foi documentada como arquitetura alvo e débito técnico da próxima sprint.

Quando essa evolução for implementada, os testes por linha de comando, Postman e Insomnia devem obter um token JWT no Keycloak a partir do Realm da aplicação e enviar:

```http
Authorization: Bearer <access_token_jwt>
```

Os escopos previstos são `lancamentos:write`, `lancamentos:read` e `consolidados:read`.

## Testes por linha de comando

### Registrar lançamento

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

Resposta esperada:

```json
{
  "id": "UUID",
  "status": "RECEBIDO",
  "mensagem": "Lancamento registrado com sucesso"
}
```

### Registrar débito

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

### Listar todos os lançamentos

```bash
curl -sS http://localhost:8080/api/lancamentos
```

### Consultar lançamentos por período

```bash
curl -sS 'http://localhost:8080/api/lancamentos?dataInicio=2026-04-21&dataFim=2026-04-21'
```

### Consultar consolidado

O processamento é assíncrono. Se consultar imediatamente após o `POST`, pode haver `404` por alguns instantes.

```bash
curl -i http://localhost:8080/api/consolidados/2026-04-21
```

Resposta esperada após o consumo do evento:

```json
{
  "dataEfetiva": "2026-04-21",
  "totalCreditos": 100.50,
  "totalDebitos": 25.10,
  "saldoFinal": 75.40,
  "ultimaAtualizacao": "2026-04-21T10:00:00Z"
}
```

### Validar consistência eventual em loop

```bash
for i in $(seq 1 20); do
  curl -sS http://localhost:8080/api/consolidados/2026-04-21
  echo
  sleep 1
done
```

### Testar validação de negócio

```bash
curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "CREDITO",
    "valor": 0,
    "dataEfetiva": "2026-04-21",
    "descricao": "Valor invalido"
  }'
```

Resposta esperada:

- HTTP `400`
- `codigo`: `VALIDACAO_NEGOCIO`

## Testes com Postman ou Insomnia

Crie uma environment com as variáveis:

| Variável | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `dataEfetiva` | `2026-04-21` |

### Request 1: Health

- Método: `GET`
- URL: `{{baseUrl}}/health`

### Request 2: Criar crédito

- Método: `POST`
- URL: `{{baseUrl}}/api/lancamentos`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "tipo": "CREDITO",
  "valor": 100.50,
  "dataEfetiva": "{{dataEfetiva}}",
  "descricao": "Venda via Postman"
}
```

Resultado esperado:

- HTTP `201`
- campo `status` igual a `RECEBIDO`

### Request 3: Criar débito

- Método: `POST`
- URL: `{{baseUrl}}/api/lancamentos`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "tipo": "DEBITO",
  "valor": 25.10,
  "dataEfetiva": "{{dataEfetiva}}",
  "descricao": "Despesa via Postman"
}
```

### Request 4: Consultar lançamentos do dia

- Método: `GET`
- URL: `{{baseUrl}}/api/lancamentos?dataInicio={{dataEfetiva}}&dataFim={{dataEfetiva}}`

### Request 5: Consultar consolidado

- Método: `GET`
- URL: `{{baseUrl}}/api/consolidados/{{dataEfetiva}}`

Caso retorne `404`, aguarde alguns segundos e envie novamente. Isso demonstra a consistência eventual do fluxo assíncrono.

### Request 6: Validação negativa

- Método: `POST`
- URL: `{{baseUrl}}/api/lancamentos`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "tipo": "CREDITO",
  "valor": 0,
  "dataEfetiva": "{{dataEfetiva}}",
  "descricao": "Teste invalido"
}
```

Resultado esperado:

- HTTP `400`
- `codigo`: `VALIDACAO_NEGOCIO`

## Testes automatizados Maven

Se Maven estiver instalado no WSL:

```bash
cd servico-lancamentos
mvn test

cd ../servico-consolidado-diario
mvn test
```

Alternativa via Docker, sem Maven instalado localmente:

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/servico-lancamentos \
  maven:3.9.9-eclipse-temurin-11 \
  mvn test

docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/servico-consolidado-diario \
  maven:3.9.9-eclipse-temurin-11 \
  mvn test
```

## Rodar serviços isoladamente

Suba apenas infra:

```bash
docker compose up -d postgres rabbitmq
```

Serviço de lançamentos:

```bash
cd servico-lancamentos
SERVER_PORT=8081 \
APP_DB_SCHEMA=lancamentos \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/controle_financeiro \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=app \
SPRING_RABBITMQ_HOST=localhost \
mvn spring-boot:run
```

Serviço de consolidado:

```bash
cd servico-consolidado-diario
SERVER_PORT=8082 \
APP_DB_SCHEMA=consolidado \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/controle_financeiro \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=app \
SPRING_RABBITMQ_HOST=localhost \
mvn spring-boot:run
```

## Teste de resiliência

Valida que lançamentos continua aceitando escrita com o consolidado parado.

```bash
BASE_URL=http://localhost:8080 \
TEST_RUN_ID=resiliencia-local-$(date +%Y%m%d%H%M%S) \
DATA_EFETIVA=2099-01-10 \
./tests/ci/resiliencia-consolidado-down.sh
```

## Teste de carga com k6

```bash
docker run --rm --network host \
  -e BASE_URL=http://localhost:8080 \
  -e RATE=50 \
  -e DURATION=1m \
  -e MAX_LOSS_RATE=0.05 \
  -e DATA_EFETIVA=2099-01-11 \
  -e TEST_RUN_ID=k6-local-$(date +%Y%m%d%H%M%S) \
  -v "$PWD/tests/load/k6:/scripts:ro" \
  grafana/k6 run /scripts/lancamentos-50rps.js
```

## Teste de carga com JMeter

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
  -JDATA_EFETIVA=2099-01-12 \
  -JTEST_RUN_ID=jmeter-local-$(date +%Y%m%d%H%M%S)
```

## Inspeção operacional

Containers:

```bash
docker compose --profile observability ps
```

Logs:

```bash
docker compose --profile observability logs -f --tail=200
```

Filas RabbitMQ:

```bash
docker exec rabbitmq-controle-financeiro rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Banco:

```bash
docker exec -it postgres-controle-financeiro psql -U app -d controle_financeiro
```

Outbox:

```sql
SELECT status, count(*)
FROM lancamentos.outbox_evento
GROUP BY status;
```
