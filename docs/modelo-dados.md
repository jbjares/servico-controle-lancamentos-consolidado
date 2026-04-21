# Modelo de Dados

O projeto usa PostgreSQL com dois schemas lógicos, versionados por Liquibase em cada serviço.

| Schema | Serviço responsável | Finalidade |
|---|---|---|
| `lancamentos` | `servico-lancamentos` | Lançamentos financeiros e Outbox |
| `consolidado` | `servico-consolidado-diario` | Consolidado diário e idempotência do consumidor |

## Schema `lancamentos`

### Tabela `lancamento`

| Coluna | Tipo | Obrigatório | Chave | Observação |
|---|---|---:|---:|---|
| `id` | `UUID` | Sim | PK | Identificador do lançamento |
| `tipo` | `VARCHAR(20)` | Sim |  | `CREDITO` ou `DEBITO` |
| `valor` | `NUMERIC(19,2)` | Sim |  | Valor monetário |
| `data_efetiva` | `DATE` | Sim |  | Data usada no consolidado |
| `descricao` | `VARCHAR(255)` | Não |  | Descrição livre |
| `criado_em` | `TIMESTAMP` | Sim |  | Criação do lançamento |

Índices:

- `idx_lancamento_data_efetiva(data_efetiva)`

### Tabela `outbox_evento`

| Coluna | Tipo | Obrigatório | Chave | Observação |
|---|---|---:|---:|---|
| `id_evento` | `UUID` | Sim | PK | Identificador do evento |
| `tipo_evento` | `VARCHAR(120)` | Sim |  | Tipo lógico do evento |
| `correlation_id` | `VARCHAR(120)` | Sim |  | Correlação da jornada |
| `exchange_name` | `VARCHAR(120)` | Sim |  | Exchange RabbitMQ |
| `routing_key` | `VARCHAR(120)` | Sim |  | Routing key RabbitMQ |
| `payload` | `TEXT` | Sim |  | JSON do evento |
| `status` | `VARCHAR(20)` | Sim |  | `PENDENTE`, `PROCESSANDO`, `PUBLICADO` ou `ERRO` |
| `tentativas` | `INT` | Sim |  | Quantidade de tentativas |
| `proxima_tentativa_em` | `TIMESTAMP` | Sim |  | Próxima tentativa elegível |
| `criado_em` | `TIMESTAMP` | Sim |  | Criação do evento |
| `atualizado_em` | `TIMESTAMP` | Sim |  | Última atualização |
| `publicado_em` | `TIMESTAMP` | Não |  | Publicação confirmada |
| `processando_por` | `VARCHAR(120)` | Não |  | Worker que reservou o evento |
| `processando_em` | `TIMESTAMP` | Não |  | Início da reserva |
| `claim_expira_em` | `TIMESTAMP` | Não |  | Expiração da reserva |
| `ultimo_erro` | `VARCHAR(1000)` | Não |  | Último erro registrado |

Índices:

- `idx_outbox_status_proxima_tentativa(status, proxima_tentativa_em, criado_em)`
- `idx_outbox_claim_expira(status, claim_expira_em, criado_em)`

## Schema `consolidado`

### Tabela `consolidado_diario`

| Coluna | Tipo | Obrigatório | Chave | Observação |
|---|---|---:|---:|---|
| `data_efetiva` | `DATE` | Sim | PK | Dia consolidado |
| `total_creditos` | `NUMERIC(19,2)` | Sim |  | Soma dos créditos |
| `total_debitos` | `NUMERIC(19,2)` | Sim |  | Soma dos débitos |
| `saldo_final` | `NUMERIC(19,2)` | Sim |  | `total_creditos - total_debitos` |
| `ultima_atualizacao` | `TIMESTAMP` | Sim |  | Última atualização |

### Tabela `evento_processado`

| Coluna | Tipo | Obrigatório | Chave | Observação |
|---|---|---:|---:|---|
| `id_evento` | `UUID` | Sim | PK | Evento já processado |
| `correlation_id` | `VARCHAR(100)` | Não |  | Correlação da jornada |
| `processado_em` | `TIMESTAMP` | Sim |  | Data/hora do processamento |

## Liquibase

Arquivos de changelog:

- `servico-lancamentos/src/main/resources/db/changelog/db.changelog-master.xml`
- `servico-consolidado-diario/src/main/resources/db/changelog/db.changelog-master.xml`

Estratégia adotada:

- cada serviço versiona o schema sob sua responsabilidade;
- `ddl-auto=validate` evita criação implícita pelo Hibernate;
- Liquibase cria schemas, tabelas e índices no startup;
- o banco único com schemas separados reduz a complexidade da POC sem apagar a separação lógica dos domínios.

## Consultas úteis

Entrar no PostgreSQL:

```bash
docker exec -it postgres-controle-financeiro psql -U app -d controle_financeiro
```

Ver lançamentos recentes:

```sql
SELECT id, tipo, valor, data_efetiva, descricao, criado_em
FROM lancamentos.lancamento
ORDER BY criado_em DESC
LIMIT 10;
```

Ver situação da Outbox:

```sql
SELECT status, count(*)
FROM lancamentos.outbox_evento
GROUP BY status
ORDER BY status;
```

Ver consolidado:

```sql
SELECT data_efetiva, total_creditos, total_debitos, saldo_final, ultima_atualizacao
FROM consolidado.consolidado_diario
ORDER BY data_efetiva DESC;
```

Ver eventos processados:

```sql
SELECT id_evento, correlation_id, processado_em
FROM consolidado.evento_processado
ORDER BY processado_em DESC
LIMIT 10;
```
