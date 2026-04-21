# JMeter - carga, stress e performance

Este plano complementa o teste k6. O k6 continua sendo o teste automatizado principal do CI para o RNF de 50 req/s e perda maxima de 5%; o JMeter fica como artefato corporativo opcional para execucao local, coleta de `.jtl` e analise em ferramentas conhecidas por times de QA/performance.

## Execucao local

Com a stack core ativa:

```bash
docker compose up --build -d
```

Execute o plano JMeter em modo nao interativo:

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

## Cenários sugeridos

- Carga: `JTHREADS=80`, `JDURATION=60`, throughput alvo de 50 req/s.
- Stress: aumentar gradualmente `JTHREADS` para 150, 250 e 400.
- Performance/latencia: manter 50 req/s por 5 a 10 minutos e avaliar p95/p99 nos relatorios JMeter e no Grafana.

O plano valida HTTP `201` e corpo contendo `RECEBIDO`.
