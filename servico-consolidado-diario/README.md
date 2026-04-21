# Serviço de Consolidado Diário

Responsável por consumir eventos de lançamentos, consolidar os valores por data e disponibilizar consulta do saldo diário.

## Responsabilidades

- Consumir `LancamentoRegistradoEvent` do RabbitMQ.
- Atualizar `ConsolidadoDiario` no schema `consolidado`.
- Registrar `EventoProcessado` para garantir idempotência.
- Consultar saldo consolidado por data efetiva.
- Ignorar com segurança eventos duplicados.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Status simples do serviço |
| `GET` | `/api/consolidados/{dataEfetiva}` | Consulta consolidado diário |
| `GET` | `/actuator/health/readiness` | Readiness |
| `GET` | `/actuator/health/liveness` | Liveness |
| `GET` | `/actuator/prometheus` | Métricas Prometheus |

## Resposta de consulta

```json
{
  "dataEfetiva": "2026-04-21",
  "totalCreditos": 100.50,
  "totalDebitos": 25.10,
  "saldoFinal": 75.40,
  "ultimaAtualizacao": "2026-04-21T10:00:00Z"
}
```

Quando não existe consolidado para a data:

- HTTP `404`
- `codigo`: `NAO_ENCONTRADO`

## Execução via Docker Compose

Na raiz do projeto:

```bash
docker compose up --build -d
```

Consulte pelo Nginx:

```bash
curl -i http://localhost:8080/api/consolidados/2026-04-21
```

Portas diretas das réplicas:

- `http://localhost:8082`
- `http://localhost:8085`

## Execução isolada

Suba PostgreSQL e RabbitMQ:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
docker compose up -d postgres rabbitmq
```

Execute o serviço:

```bash
cd servico-consolidado-diario
SERVER_PORT=8082 \
APP_DB_SCHEMA=consolidado \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/controle_financeiro \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=app \
SPRING_RABBITMQ_HOST=localhost \
SPRING_RABBITMQ_PORT=5672 \
SPRING_RABBITMQ_USERNAME=guest \
SPRING_RABBITMQ_PASSWORD=guest \
mvn spring-boot:run
```

## Testes Maven

```bash
mvn test
```

Sem Maven local:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/servico-consolidado-diario \
  maven:3.9.9-eclipse-temurin-11 \
  mvn test
```

## Testes com Postman ou Insomnia

Crie uma variável `baseUrl`:

```text
http://localhost:8080
```

Consultar consolidado:

- Método: `GET`
- URL: `{{baseUrl}}/api/consolidados/2026-04-21`

Observação: o consolidado é atualizado de forma assíncrona. Após criar um lançamento, aguarde alguns segundos ou reenvie a consulta até o evento ser consumido.

## Banco e mensageria

- Schema: `consolidado`
- Tabela principal: `consolidado_diario`
- Tabela de idempotência: `evento_processado`
- Fila consumida: `lancamento.registrado.fila`
- DLQ: `lancamento.registrado.dlq`
