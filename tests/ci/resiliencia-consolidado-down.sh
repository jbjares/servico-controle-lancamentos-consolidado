#!/usr/bin/env bash
set -euo pipefail

RUN_ID="${TEST_RUN_ID:-ci-rnf-consolidado-down-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}}"
DATA_EFETIVA="${DATA_EFETIVA:-2099-01-02}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
CONSOLIDADO_SERVICES="${CONSOLIDADO_SERVICES:-servico-consolidado-diario-a servico-consolidado-diario-b}"
CONSOLIDADO_READINESS_URLS="${CONSOLIDADO_READINESS_URLS:-http://localhost:8082/actuator/health/readiness http://localhost:8085/actuator/health/readiness}"
VALOR="456.78"

cleanup() {
  docker compose start $CONSOLIDADO_SERVICES >/dev/null 2>&1 || true
  docker exec -i postgres-controle-financeiro psql -U app -d controle_financeiro >/dev/null 2>&1 <<SQL || true
WITH ids AS (
    SELECT id FROM lancamentos.lancamento
    WHERE data_efetiva = DATE '${DATA_EFETIVA}'
      AND descricao = '${RUN_ID}'
)
DELETE FROM consolidado.evento_processado ep
USING ids
WHERE ep.correlation_id = 'corr-' || ids.id::text;

DELETE FROM lancamentos.outbox_evento
WHERE payload LIKE '%${RUN_ID}%';

DELETE FROM lancamentos.lancamento
WHERE data_efetiva = DATE '${DATA_EFETIVA}'
  AND descricao = '${RUN_ID}';

DELETE FROM consolidado.consolidado_diario
WHERE data_efetiva = DATE '${DATA_EFETIVA}';
SQL
}
trap cleanup EXIT

echo "Parando servico-consolidado-diario para validar independencia do servico-lancamentos..."
docker compose stop $CONSOLIDADO_SERVICES
sleep 5

payload=$(printf '{"tipo":"CREDITO","valor":%s,"dataEfetiva":"%s","descricao":"%s"}' "$VALOR" "$DATA_EFETIVA" "$RUN_ID")
http_code=$(curl -sS -o /tmp/lancamento-response.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "$payload" \
  "${BASE_URL}/api/lancamentos")

echo "HTTP status com consolidado parado: $http_code"
cat /tmp/lancamento-response.json

if [ "$http_code" != "201" ]; then
  echo "Falha: servico-lancamentos nao retornou 201 com consolidado parado" >&2
  exit 1
fi

sleep 4

echo "Estado das filas com consolidado parado:"
docker exec rabbitmq-controle-financeiro rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers

outbox_status=$(docker exec -i postgres-controle-financeiro psql -U app -d controle_financeiro -t -A <<SQL
SELECT status || '|' || count(*)
FROM lancamentos.outbox_evento
WHERE payload LIKE '%${RUN_ID}%'
GROUP BY status
ORDER BY status;
SQL
)
echo "Outbox com consolidado parado: ${outbox_status}"

if ! grep -q 'PUBLICADO|1' <<<"$outbox_status"; then
  echo "Falha: evento deveria ter sido publicado no RabbitMQ mesmo com consolidado parado" >&2
  exit 1
fi

echo "Religando servico-consolidado-diario..."
docker compose start $CONSOLIDADO_SERVICES
./tests/ci/wait-url.sh $CONSOLIDADO_READINESS_URLS

for _ in $(seq 1 45); do
  if curl -fsS "http://localhost:8082/api/consolidados/${DATA_EFETIVA}" 2>/dev/null | grep -q "$VALOR"; then
    echo "Consolidado processou evento apos retorno."
    exit 0
  fi
  sleep 2
done

echo "Falha: consolidado nao processou o evento apos retornar" >&2
exit 1
