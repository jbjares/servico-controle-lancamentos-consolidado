import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

export const taxaErroNegocio = new Rate('taxa_erro_negocio');

const rate = Number(__ENV.RATE || 50);
const duration = __ENV.DURATION || '1m';
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const dataEfetiva = __ENV.DATA_EFETIVA || new Date().toISOString().slice(0, 10);
const testRunId = __ENV.TEST_RUN_ID || `k6-${Date.now()}`;
const maxLossRate = Number(__ENV.MAX_LOSS_RATE || 0.05);
const expectedIterations = Math.ceil(rate * durationToSeconds(duration));
const maxDroppedIterations = Math.floor(expectedIterations * maxLossRate);

export const options = {
  scenarios: {
    lancamentos_50rps: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 100),
      maxVUs: Number(__ENV.MAX_VUS || 200),
    },
  },
  thresholds: {
    http_req_failed: [`rate<${maxLossRate}`],
    taxa_erro_negocio: [`rate<${maxLossRate}`],
    dropped_iterations: [`count<=${maxDroppedIterations}`],
  },
};

export default function () {
  const payload = JSON.stringify({
    tipo: Math.random() >= 0.5 ? 'CREDITO' : 'DEBITO',
    valor: Number((Math.random() * 1000 + 1).toFixed(2)),
    dataEfetiva,
    descricao: `${testRunId}-vu-${__VU}-iter-${__ITER}`,
  });

  const response = http.post(`${baseUrl}/api/lancamentos`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'POST /api/lancamentos' },
  });

  let body = {};
  try {
    body = response.json();
  } catch (_) {
    body = {};
  }

  const ok = check(response, {
    'status 201': (r) => r.status === 201,
  }) && check(body, {
    'response has id': (b) => Boolean(b.id),
    'status RECEBIDO': (b) => b.status === 'RECEBIDO',
  });

  taxaErroNegocio.add(!ok);
}

function durationToSeconds(value) {
  const match = String(value).trim().match(/^(\d+)(ms|s|m|h)$/);
  if (!match) {
    throw new Error(`Duracao invalida para DURATION: ${value}`);
  }

  const amount = Number(match[1]);
  const unit = match[2];
  if (unit === 'ms') {
    return amount / 1000;
  }
  if (unit === 's') {
    return amount;
  }
  if (unit === 'm') {
    return amount * 60;
  }
  return amount * 3600;
}
