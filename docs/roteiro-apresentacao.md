# Roteiro de Apresentação

Este roteiro organiza a apresentação do desafio de forma objetiva, começando pelo problema de negócio, passando pelas decisões arquiteturais e encerrando com demonstração funcional, testes e próximos passos.

## 1. Abertura

Mensagem sugerida:

> A solução foi construída para atender ao controle de fluxo de caixa diário de um comerciante, separando o registro de lançamentos do cálculo do consolidado diário. O foco foi demonstrar domínio arquitetural, resiliência, consistência eventual, testabilidade e clareza de evolução para produção.

Pontos principais:

- problema: registrar créditos e débitos;
- necessidade: consultar saldo diário consolidado;
- requisito crítico: lançamentos não pode ficar indisponível se consolidado cair;
- pico esperado: consolidado deve suportar 50 requisições por segundo com no máximo 5% de perda.

## 2. Visão da Solução

Explique os componentes:

- `servico-lancamentos`: recebe, valida e persiste lançamentos;
- `servico-consolidado-diario`: consome eventos e atualiza o saldo diário;
- PostgreSQL: persistência com schemas separados;
- RabbitMQ: integração assíncrona;
- Outbox Pattern: garantia entre gravação no banco e publicação de evento;
- Nginx: ponto único de entrada e load balancing entre réplicas;
- Prometheus/Grafana: observabilidade opcional;
- k6/JMeter: validação de carga e performance.

Mensagem de apoio:

> A arquitetura separa responsabilidades e evita acoplamento síncrono. O lançamento é aceito e persistido mesmo que o consolidado esteja fora do ar, porque a integração ocorre via Outbox e RabbitMQ.

## 3. Decisões Arquiteturais

Mostre os trade-offs:

| Decisão | Justificativa |
|---|---|
| Dois serviços | Isolamento de falhas e escala independente |
| RabbitMQ | Suficiente para eventos discretos e mais simples que Kafka para a POC |
| Outbox Pattern | Evita perda de evento quando banco e broker têm ciclos diferentes |
| Consistência eventual | Permite disponibilidade maior do fluxo de lançamento |
| PostgreSQL com schemas | Mantém isolamento lógico com execução local simples |
| Docker Compose profiles | Evita subir infraestrutura excessiva para validar o core |
| JWT/Keycloak documentado | Segurança corporativa planejada sem tornar a POC pesada |

## 4. Segurança

Explique claramente:

- nesta POC, autenticação/autorização não está ativa em runtime;
- isso foi assumido como débito técnico porque não era requisito obrigatório;
- a arquitetura alvo usa JWT Bearer;
- Keycloak é a ferramenta de IAM/OAuth2/OIDC;
- Realm é onde serão cadastrados usuários, clients, roles, groups, scopes e metadados de autenticação/autorização.

Mensagem de apoio:

> O desenho de segurança está definido para a próxima sprint: os serviços passam a atuar como OAuth2 Resource Servers, validando tokens emitidos pelo Keycloak a partir do Realm da aplicação.

## 5. Demonstração Funcional

Antes da apresentação, subir a stack recomendada:

```bash
cd /home/jbjares/workspaces/carrefour/servico-controle-lancamentos-consolidado
docker compose --profile observability up --build -d
```

Aguardar readiness:

```bash
./tests/ci/wait-url.sh \
  http://localhost:8080/health \
  http://localhost:8081/actuator/health/readiness \
  http://localhost:8084/actuator/health/readiness \
  http://localhost:8082/actuator/health/readiness \
  http://localhost:8085/actuator/health/readiness \
  http://localhost:9090/-/ready \
  http://localhost:3000/api/health
```

Fluxo para mostrar:

1. Abrir `http://localhost:8080/health`.
2. Abrir RabbitMQ em `http://localhost:15672`.
3. Abrir Prometheus em `http://localhost:9090`.
4. Abrir Grafana em `http://localhost:3000`.
5. Criar um lançamento de crédito.
6. Criar um lançamento de débito.
7. Consultar lançamentos do dia.
8. Consultar consolidado diário.

Comandos úteis:

```bash
DATA_EFETIVA=$(date +%F)

curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d "{
    \"tipo\": \"CREDITO\",
    \"valor\": 100.50,
    \"dataEfetiva\": \"${DATA_EFETIVA}\",
    \"descricao\": \"Venda demonstracao\"
  }"

curl -i -X POST http://localhost:8080/api/lancamentos \
  -H 'Content-Type: application/json' \
  -d "{
    \"tipo\": \"DEBITO\",
    \"valor\": 25.10,
    \"dataEfetiva\": \"${DATA_EFETIVA}\",
    \"descricao\": \"Despesa demonstracao\"
  }"

curl -sS "http://localhost:8080/api/lancamentos?dataInicio=${DATA_EFETIVA}&dataFim=${DATA_EFETIVA}"

curl -i "http://localhost:8080/api/consolidados/${DATA_EFETIVA}"
```

Se o consolidado retornar `404` imediatamente após o lançamento, explique a consistência eventual e consulte novamente após alguns segundos.

## 6. Testes e Evidências

Mostre que a solução foi validada por múltiplas camadas:

- testes unitários e integração com H2 nos dois serviços;
- `docker compose --profile full-ops config`;
- readiness de todos os containers;
- smoke test funcional via Nginx;
- teste de resiliência com consolidado parado;
- k6 a 50 requisições por segundo;
- JMeter disponível como evidência adicional para times QA.

Mensagem de apoio:

> A regressão cobre regras de domínio, endpoints, Outbox, consumidor, idempotência e o RNF principal de disponibilidade do serviço de lançamentos.

## 7. Observabilidade

Mostre os três modos:

- `core`: banco, broker, réplicas e Nginx;
- `observability`: core + Prometheus + Grafana;
- `full-ops`: stack completa com logs, alertas e exporters.

Explique que o modo padrão é enxuto, e a stack completa existe para demonstrar maturidade operacional sem obrigar o avaliador a subir tudo.

## 8. Encerramento

Mensagem sugerida:

> A entrega prioriza o fluxo de negócio, a separação de capacidades, a resiliência e a testabilidade. O que ficou fora do runtime, como autenticação/autorização com Keycloak, foi documentado como evolução planejada, com escopo claro para a próxima sprint.

## 9. Perguntas Esperadas

| Pergunta | Resposta curta |
|---|---|
| Por que não chamada síncrona entre serviços? | Porque quebraria o requisito de disponibilidade quando o consolidado cair. |
| Por que Outbox? | Para garantir que lançamento salvo gere evento publicável, mesmo com falha temporária do RabbitMQ. |
| Por que RabbitMQ e não Kafka? | RabbitMQ atende bem eventos discretos com menor custo operacional para a POC. |
| Por que Keycloak não está rodando? | Segurança não era requisito obrigatório; está documentada como próxima sprint para não inflar a POC. |
| Como provar 50 req/s? | Pelo teste k6 curto no CI e pelo roteiro local configurável. |
| O consolidado pode atrasar? | Sim, por consistência eventual; o lançamento continua disponível e o evento será processado depois. |

