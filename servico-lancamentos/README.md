# Serviço de Lançamentos

Responsável por receber, validar, persistir e registrar eventos de lançamentos financeiros no Outbox.

## Responsabilidades

- Criar lançamentos de crédito e débito.
- Consultar lançamentos por período.
- Persistir `Lancamento` no schema `lancamentos`.
- Registrar `LancamentoRegistradoEvent` na tabela `lancamentos.outbox_evento`.
- Publicar eventos no RabbitMQ via publisher assíncrono do Outbox.
- Continuar disponível mesmo que o serviço de consolidado diário esteja indisponível.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Status simples do serviço |
| `POST` | `/api/lancamentos` | Cria um lançamento |
| `GET` | `/api/lancamentos` | Lista todos os lançamentos |
| `GET` | `/api/lancamentos?dataInicio=YYYY-MM-DD&dataFim=YYYY-MM-DD` | Consulta lançamentos por período |
| `GET` | `/actuator/health/readiness` | Readiness |
| `GET` | `/actuator/health/liveness` | Liveness |
| `GET` | `/actuator/prometheus` | Métricas Prometheus |

## Segurança

Autenticação/autorização ainda não está ativa neste serviço durante a POC. A arquitetura alvo prevê proteção dos endpoints de negócio com JWT Bearer emitido pelo Keycloak a partir do Realm da aplicação, onde serão cadastrados usuários, clients, roles, groups, scopes e metadados de autenticação/autorização.

Escopos previstos:

- `lancamentos:write` para criação de lançamentos;
- `lancamentos:read` para consulta de lançamentos.

Essa implementação está registrada como débito técnico da próxima sprint.

## Payload de criação

```json
{
  "tipo": "CREDITO",
  "valor": 100.50,
  "dataEfetiva": "2026-04-21",
  "descricao": "Venda no caixa"
}
```

Valores aceitos para `tipo`:

- `CREDITO`
- `DEBITO`

## Resposta de criação

```json
{
  "id": "UUID",
  "status": "RECEBIDO",
  "mensagem": "Lancamento registrado com sucesso"
}
```

## Execução via Docker Compose

Na raiz do projeto:

```bash
docker compose up --build -d
```

Acesse a API preferencialmente pelo Nginx:

```bash
curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "CREDITO",
    "valor": 100.50,
    "dataEfetiva": "2026-04-21",
    "descricao": "Teste via Nginx"
  }'
```

Portas diretas das réplicas:

- `http://localhost:8081`
- `http://localhost:8084`

## Execução isolada

Suba PostgreSQL e RabbitMQ:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
docker compose up -d postgres rabbitmq
```

Execute o serviço:

```bash
cd servico-lancamentos
SERVER_PORT=8081 \
APP_DB_SCHEMA=lancamentos \
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
  -w /workspace/servico-lancamentos \
  maven:3.9.9-eclipse-temurin-11 \
  mvn test
```

## Testes com Postman ou Insomnia

Crie uma variável `baseUrl`:

```text
http://localhost:8080
```

Criar lançamento:

- Método: `POST`
- URL: `{{baseUrl}}/api/lancamentos`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "tipo": "DEBITO",
  "valor": 25.10,
  "dataEfetiva": "2026-04-21",
  "descricao": "Despesa via Postman"
}
```

Consultar por período:

- Método: `GET`
- URL: `{{baseUrl}}/api/lancamentos?dataInicio=2026-04-21&dataFim=2026-04-21`

## Banco e mensageria

- Schema: `lancamentos`
- Tabela principal: `lancamento`
- Tabela Outbox: `outbox_evento`
- Exchange: `lancamentos.exchange`
- Routing key: `lancamento.registrado`
- Fila: `lancamento.registrado.fila`
- DLQ: `lancamento.registrado.dlq`
