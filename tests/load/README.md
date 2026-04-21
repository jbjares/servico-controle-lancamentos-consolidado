# Testes de carga com k6

Este teste valida o requisito operacional de 50 requisições por segundo no endpoint de criação de lançamentos. O script falha quando a taxa de erro HTTP, a taxa de erro de negócio ou a quantidade de `dropped_iterations` ultrapassa a perda máxima configurada. O padrão é 5%.

Para uma visão completa de execução por CLI, Postman, Insomnia, k6 e JMeter, consulte também [`../../docs/execucao-testes.md`](../../docs/execucao-testes.md).

## Execução local no WSL

Com os containers da aplicação saudáveis:

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

## Variáveis principais

- `BASE_URL`: URL base da API, preferencialmente via load balancer. Padrão: `http://localhost:8080`.
- `RATE`: taxa alvo por segundo. Padrão: `50`.
- `DURATION`: duração do teste. Padrão: `1m`.
- `MAX_LOSS_RATE`: perda máxima aceita para erros e iterações descartadas. Padrão: `0.05`.
- `PRE_ALLOCATED_VUS`: VUs pré-alocados pelo k6. Padrão: `100`.
- `MAX_VUS`: limite máximo de VUs. Padrão: `200`.
- `DATA_EFETIVA`: data gravada nos lançamentos de teste.
- `TEST_RUN_ID`: prefixo gravado no campo `descricao` para rastrear e limpar massa de teste.

## Validação pós-carga

Após um teste com `TEST_RUN_ID` e `DATA_EFETIVA` conhecidos, valide se os eventos foram criados, publicados e processados:

```bash
TEST_RUN_ID=k6-local-20260421120000 \
DATA_EFETIVA=2099-01-11 \
EXPECTED_ITERATIONS=3000 \
MAX_LOSS_RATE=0.05 \
./tests/ci/verify-k6-outbox-and-cleanup.sh
```

O script também remove a massa de teste ao final.

## JMeter

O plano JMeter fica em [`jmeter/lancamentos-carga-stress-performance.jmx`](jmeter/lancamentos-carga-stress-performance.jmx) e pode ser usado como evidência complementar de carga, stress e performance.
