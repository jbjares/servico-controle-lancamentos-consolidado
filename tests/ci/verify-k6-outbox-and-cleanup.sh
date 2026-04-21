#!/usr/bin/env bash
set -euo pipefail

RUN_ID="${TEST_RUN_ID:?TEST_RUN_ID e obrigatorio}"
DATA_EFETIVA="${DATA_EFETIVA:?DATA_EFETIVA e obrigatorio}"
EXPECTED_ITERATIONS="${EXPECTED_ITERATIONS:?EXPECTED_ITERATIONS e obrigatorio}"
MAX_LOSS_RATE="${MAX_LOSS_RATE:-0.05}"
MIN_EXPECTED=$(python3 - <<PY
import math
expected = int("${EXPECTED_ITERATIONS}")
loss = float("${MAX_LOSS_RATE}")
print(math.floor(expected * (1 - loss)))
PY
)

cleanup() {
  docker exec -i postgres-controle-financeiro psql -U app -d controle_financeiro >/dev/null 2>&1 <<SQL || true
WITH ids AS (
    SELECT id FROM lancamentos.lancamento
    WHERE data_efetiva = DATE '${DATA_EFETIVA}'
      AND descricao LIKE '${RUN_ID}%'
)
DELETE FROM consolidado.evento_processado ep
USING ids
WHERE ep.correlation_id = 'corr-' || ids.id::text;

DELETE FROM lancamentos.outbox_evento
WHERE payload LIKE '%${RUN_ID}%';

DELETE FROM lancamentos.lancamento
WHERE data_efetiva = DATE '${DATA_EFETIVA}'
  AND descricao LIKE '${RUN_ID}%';

DELETE FROM consolidado.consolidado_diario
WHERE data_efetiva = DATE '${DATA_EFETIVA}';
SQL
}
trap cleanup EXIT

query_counts() {
  docker exec -i postgres-controle-financeiro psql -U app -d controle_financeiro -t -A <<SQL
WITH lancamentos_teste AS (
    SELECT id
    FROM lancamentos.lancamento
    WHERE data_efetiva = DATE '${DATA_EFETIVA}'
      AND descricao LIKE '${RUN_ID}%'
), outbox_teste AS (
    SELECT status
    FROM lancamentos.outbox_evento
    WHERE payload LIKE '%${RUN_ID}%'
), eventos_processados AS (
    SELECT ep.id_evento
    FROM consolidado.evento_processado ep
    JOIN lancamentos_teste lt ON ep.correlation_id = 'corr-' || lt.id::text
)
SELECT
    (SELECT count(*) FROM lancamentos_teste) || '|' ||
    (SELECT count(*) FROM outbox_teste WHERE status = 'PUBLICADO') || '|' ||
    (SELECT count(*) FROM outbox_teste WHERE status = 'ERRO') || '|' ||
    (SELECT count(*) FROM eventos_processados);
SQL
}

last_counts=""
for _ in $(seq 1 90); do
  last_counts=$(query_counts)
  IFS='|' read -r created published errors processed <<<"$last_counts"
  echo "k6/outbox counts: created=$created published=$published errors=$errors processed=$processed min_expected=$MIN_EXPECTED"

  if [ "$created" -ge "$MIN_EXPECTED" ] && [ "$published" -ge "$MIN_EXPECTED" ] && [ "$processed" -ge "$MIN_EXPECTED" ] && [ "$errors" -eq 0 ]; then
    echo "Outbox e consumidor atenderam o limite de perda configurado."
    docker exec rabbitmq-controle-financeiro rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
    exit 0
  fi
  sleep 2
done

echo "Falha: eventos k6 nao atingiram minimo esperado. Ultimos contadores: $last_counts" >&2
docker exec rabbitmq-controle-financeiro rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers || true
exit 1
