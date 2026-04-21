# Segurança

Este documento descreve a estratégia recomendada para atender aos critérios de segurança do desafio sem transformar a POC em uma plataforma excessivamente complexa.

## Objetivo

Proteger os dados e serviços contra acessos indevidos, vazamento de informações, abuso de APIs e falhas operacionais comuns, mantendo a solução simples de executar localmente.

## Estratégia recomendada para a POC

Para esta etapa, a recomendação é demonstrar segurança em três camadas:

1. Segurança de borda
2. Segurança de aplicação
3. Segurança operacional e de dados

## Segurança de borda

O Nginx é o ponto único de entrada para as APIs.

Medidas recomendadas:

- publicar externamente apenas a porta `8080`;
- manter portas diretas das réplicas apenas para diagnóstico local;
- aplicar headers de segurança no Nginx;
- adicionar rate limiting por rota crítica;
- preparar configuração TLS para ambientes produtivos;
- centralizar autenticação em API Gateway ou Identity Provider em ambiente corporativo.

Para uma POC local, TLS pode ser documentado como requisito de ambiente, pois certificados locais autoassinados tendem a adicionar atrito para o avaliador.

## Autenticação

Opções por maturidade:

| Opção | Uso recomendado | Justificativa |
|---|---|---|
| API Key simples | POC protegida sem alta complexidade | Fácil de validar e integrar nos testes |
| Basic Auth no Nginx | Proteção local rápida | Útil para RabbitMQ/Grafana, não ideal para APIs corporativas |
| OAuth2/OIDC com JWT | Produção | Integra com Keycloak, Azure AD, Okta ou outro IdP corporativo |

Recomendação para evolução curta da POC:

- implementar API Key obrigatória nas rotas de negócio;
- manter Actuator exposto apenas com endpoints mínimos;
- deixar OAuth2/OIDC documentado como evolução produtiva.

Recomendação para produção:

- OAuth2/OIDC com JWT;
- escopos como `lancamentos:write`, `lancamentos:read` e `consolidados:read`;
- validação de issuer, audience, expiração e assinatura;
- rotação de chaves via JWKS.

## Autorização

Papéis sugeridos:

| Papel | Permissões |
|---|---|
| `OPERADOR_CAIXA` | Registrar lançamentos e consultar lançamentos |
| `GESTOR_FINANCEIRO` | Consultar consolidado diário e relatórios |
| `ADMIN_OPERACIONAL` | Consultar health, métricas e dashboards operacionais |

## Proteção contra abuso

Medidas recomendadas:

- rate limiting no Nginx para `POST /api/lancamentos`;
- limite de tamanho de payload;
- timeout de leitura e escrita;
- validação forte de DTOs;
- tratamento padronizado de erros sem stack trace em resposta HTTP;
- logs estruturados sem dados sensíveis;
- DLQ para eventos inválidos ou falhas permanentes de consumo.

## Criptografia

Para a POC:

- credenciais ficam em variáveis de ambiente do Docker Compose;
- tráfego interno roda em rede Docker isolada;
- TLS fica documentado como requisito produtivo.

Para produção:

- TLS externo obrigatório;
- TLS interno entre serviços quando exigido pela política corporativa;
- criptografia em repouso no banco gerenciada pela infraestrutura;
- senhas, tokens e chaves em secret manager;
- rotação periódica de credenciais.

## Dados sensíveis

Os dados atuais são de baixo teor sensível, mas ainda devem ser tratados com cuidado.

Diretrizes:

- não logar payload completo em erro;
- mascarar identificadores quando necessário;
- evitar dados pessoais na descrição do lançamento;
- aplicar retenção de logs;
- aplicar backup e restore testado para PostgreSQL.

## Actuator e observabilidade

Expor apenas o necessário:

- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/prometheus`

Em produção, recomenda-se restringir Actuator por rede, autenticação ou API Gateway.

## Decisão para o desafio

Para equilibrar clareza e segurança, a solução deve:

- documentar OAuth2/OIDC como arquitetura alvo;
- implementar ou preparar API Key como proteção simples de POC;
- manter Nginx como camada de entrada;
- usar rate limiting e headers de segurança no Nginx;
- não adicionar Keycloak por padrão para não aumentar excessivamente a infraestrutura local.

Essa decisão demonstra maturidade arquitetural: o desenho reconhece o caminho produtivo, mas preserva a simplicidade necessária para avaliação local.
