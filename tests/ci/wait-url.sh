#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -eq 0 ]; then
  echo "Uso: $0 URL [URL...]" >&2
  exit 2
fi

for url in "$@"; do
  echo "Aguardando $url"
  ok=0
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      ok=1
      break
    fi
    sleep 5
  done

  if [ "$ok" -ne 1 ]; then
    echo "Servico nao ficou pronto: $url" >&2
    docker compose ps || true
    docker compose logs --tail=200 || true
    exit 1
  fi
done
