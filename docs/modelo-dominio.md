# Modelo de Domínio

Este documento descreve o modelo de domínio implementado nos dois serviços Spring Boot.

## Contextos

| Contexto | Serviço | Responsabilidade |
|---|---|---|
| Lançamentos financeiros | `servico-lancamentos` | Receber, validar, persistir e emitir eventos de lançamentos de crédito e débito |
| Consolidado diário | `servico-consolidado-diario` | Consumir eventos de lançamentos e manter o saldo diário consolidado |

## Entidade `Lancamento`

Representa um débito ou crédito efetivo no fluxo de caixa.

| Campo | Tipo Java | Obrigatório | Descrição |
|---|---|---:|---|
| `id` | `UUID` | Sim | Identificador único do lançamento |
| `tipo` | `TipoLancamento` | Sim | `CREDITO` ou `DEBITO` |
| `valor` | `BigDecimal` | Sim | Valor monetário, sempre maior que zero |
| `dataEfetiva` | `LocalDate` | Sim | Data usada no consolidado diário |
| `descricao` | `String` | Não | Descrição livre do lançamento |
| `criadoEm` | `OffsetDateTime` | Sim | Data e hora de criação |

Regras principais:

- `tipo` é obrigatório.
- `valor` é obrigatório e deve ser maior que zero.
- `dataEfetiva` é obrigatória.
- `descricao` é opcional e limitada a 255 caracteres no banco.

## Enum `TipoLancamento`

Valores aceitos:

- `CREDITO`
- `DEBITO`

## Entidade `OutboxEvent`

Representa o evento persistido no padrão Outbox antes da publicação no RabbitMQ.

| Campo | Tipo Java | Obrigatório | Descrição |
|---|---|---:|---|
| `idEvento` | `UUID` | Sim | Identificador único do evento |
| `tipoEvento` | `String` | Sim | Nome lógico do evento, hoje `LancamentoRegistradoEvent` |
| `correlationId` | `String` | Sim | Correlação da jornada do lançamento |
| `exchangeName` | `String` | Sim | Exchange RabbitMQ de destino |
| `routingKey` | `String` | Sim | Routing key de destino |
| `payload` | `String` | Sim | JSON serializado do evento |
| `status` | `OutboxStatus` | Sim | Estado de publicação |
| `tentativas` | `Integer` | Sim | Número de tentativas de publicação |
| `proximaTentativaEm` | `OffsetDateTime` | Sim | Momento da próxima tentativa |
| `criadoEm` | `OffsetDateTime` | Sim | Data/hora de criação |
| `atualizadoEm` | `OffsetDateTime` | Sim | Última atualização do registro |
| `publicadoEm` | `OffsetDateTime` | Não | Momento da publicação confirmada |
| `processandoPor` | `String` | Não | Worker que reservou o evento |
| `processandoEm` | `OffsetDateTime` | Não | Momento da reserva |
| `claimExpiraEm` | `OffsetDateTime` | Não | Expiração da reserva para reprocessamento seguro |
| `ultimoErro` | `String` | Não | Último erro de publicação |

## Enum `OutboxStatus`

Valores aceitos:

- `PENDENTE`: evento gravado e aguardando publicação.
- `PROCESSANDO`: evento reservado por uma réplica do publisher.
- `PUBLICADO`: evento publicado com confirmação do RabbitMQ.
- `ERRO`: evento com falha temporária e elegível para nova tentativa.

## Evento `LancamentoRegistradoEvent`

Contrato publicado pelo `servico-lancamentos` e consumido pelo `servico-consolidado-diario`.

| Campo | Tipo Java | Obrigatório | Descrição |
|---|---|---:|---|
| `idEvento` | `UUID` | Sim | Id único do evento para idempotência |
| `correlationId` | `String` | Sim | Correlação entre lançamento, outbox e processamento |
| `idLancamento` | `UUID` | Sim | Id do lançamento original |
| `tipo` | `TipoLancamento` | Sim | `CREDITO` ou `DEBITO` |
| `valor` | `BigDecimal` | Sim | Valor do lançamento |
| `dataEfetiva` | `LocalDate` | Sim | Data a consolidar |
| `descricao` | `String` | Não | Descrição original do lançamento |
| `ocorridoEm` | `OffsetDateTime` | Sim | Momento do fato/evento |
| `versaoEvento` | `Integer` | Sim | Versão do contrato, atualmente `1` |

Exemplo:

```json
{
  "idEvento": "9dba5453-8d1d-49cb-9774-5f7a3553f596",
  "correlationId": "corr-8d4049d3-5de8-4d6b-9c1d-bf62c0b8fb21",
  "idLancamento": "8d4049d3-5de8-4d6b-9c1d-bf62c0b8fb21",
  "tipo": "CREDITO",
  "valor": 100.50,
  "dataEfetiva": "2026-04-21",
  "descricao": "Venda no caixa",
  "ocorridoEm": "2026-04-21T10:00:00Z",
  "versaoEvento": 1
}
```

## Entidade `ConsolidadoDiario`

Representa o saldo consolidado de um dia.

| Campo | Tipo Java | Obrigatório | Descrição |
|---|---|---:|---|
| `dataEfetiva` | `LocalDate` | Sim | Chave natural do consolidado |
| `totalCreditos` | `BigDecimal` | Sim | Soma dos créditos no dia |
| `totalDebitos` | `BigDecimal` | Sim | Soma dos débitos no dia |
| `saldoFinal` | `BigDecimal` | Sim | `totalCreditos - totalDebitos` |
| `ultimaAtualizacao` | `OffsetDateTime` | Sim | Momento da última atualização |

## Entidade `EventoProcessado`

Representa o controle de idempotência do consumidor.

| Campo | Tipo Java | Obrigatório | Descrição |
|---|---|---:|---|
| `idEvento` | `UUID` | Sim | Id do evento processado |
| `correlationId` | `String` | Não | Correlação do evento |
| `processadoEm` | `OffsetDateTime` | Sim | Data/hora de processamento bem-sucedido |

## Fluxo de Domínio

1. O cliente registra um lançamento.
2. O `servico-lancamentos` valida e persiste `Lancamento`.
3. Na mesma transação, grava `OutboxEvent` com `status=PENDENTE`.
4. O publisher do Outbox reserva o evento como `PROCESSANDO`.
5. Após confirmação do RabbitMQ, marca o evento como `PUBLICADO`.
6. O `servico-consolidado-diario` consome `LancamentoRegistradoEvent`.
7. O consumidor verifica `EventoProcessado`.
8. Se o evento for novo, atualiza `ConsolidadoDiario` e registra `EventoProcessado`.
9. Se o evento já tiver sido processado, ignora com segurança.
