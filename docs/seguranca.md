# Segurança

Este documento descreve a estratégia recomendada para atender aos critérios de segurança do desafio sem transformar a POC em uma plataforma excessivamente complexa.

## Objetivo

Proteger os dados e serviços contra acessos indevidos, vazamento de informações, abuso de APIs e falhas operacionais comuns, mantendo a solução simples de executar localmente.

## Status nesta entrega

A segurança de autenticação e autorização foi mantida como **decisão arquitetural documentada** e **débito técnico planejado para a próxima sprint**.

Os endpoints de negócio da POC ainda não aplicam validação runtime de token JWT. A decisão foi intencional: como autenticação/autorização não é requisito obrigatório do desafio, a entrega prioriza o fluxo financeiro, a resiliência entre serviços, Outbox, testes, observabilidade e execução local simples.

A arquitetura alvo, entretanto, está definida: as APIs devem ser protegidas com token JWT do tipo Bearer, emitido pelo Keycloak, validado pelas aplicações Spring Boot e autorizado por escopos/papéis de negócio.

Neste contexto, **Keycloak** é a ferramenta de IAM/OAuth2/OIDC. O **Realm** é o domínio lógico dentro do Keycloak onde serão cadastrados e administrados os metadados de autenticação e autorização da aplicação, incluindo usuários, clients, roles, groups, scopes, políticas de acesso e chaves de assinatura.

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

### Arquitetura alvo com JWT Bearer e Keycloak

Na próxima sprint, os serviços devem operar como **OAuth2 Resource Servers**. Todo endpoint de negócio exposto pela borda da aplicação deve exigir:

```http
Authorization: Bearer <access_token_jwt>
```

O token deve ser obtido no **Realm da aplicação dentro do Keycloak**, por exemplo `controle-lancamentos`, usando fluxos OIDC/OAuth2 compatíveis com o tipo de consumidor:

| Consumidor | Fluxo recomendado | Observação |
|---|---|---|
| Usuário humano via UI | Authorization Code com PKCE | Recomendado para aplicações web modernas |
| Integração sistema-sistema | Client Credentials | Recomendado para automações e integrações internas |
| Testes locais/Postman/Insomnia | Client Credentials ou usuário técnico | Facilita validação sem expor senha em código |

As aplicações devem validar:

- assinatura do token por chaves públicas do Keycloak via JWKS;
- `issuer` do Realm esperado;
- `audience`/cliente esperado;
- expiração (`exp`) e validade temporal (`nbf`, quando presente);
- escopos e papéis necessários para a rota chamada.

O acesso seguro deve usar TLS em ambientes reais. As chaves criptográficas de assinatura devem ser gerenciadas pelo Keycloak, com rotação controlada. Caso haja requisito de confidencialidade do conteúdo do token, a evolução pode considerar JWE; para a maior parte dos cenários de API, JWT assinado via JWS, trafegando por TLS, é suficiente e mais simples de operar.

### Opções por maturidade

| Opção | Uso recomendado | Justificativa |
|---|---|---|
| API Key simples | POC protegida sem alta complexidade | Fácil de validar e integrar nos testes |
| Basic Auth no Nginx | Proteção local rápida | Útil para RabbitMQ/Grafana, não ideal para APIs corporativas |
| OAuth2/OIDC com JWT | Produção | Integra com Keycloak, Azure AD, Okta ou outro IdP corporativo |

Recomendação para a próxima sprint:

- adicionar Spring Security OAuth2 Resource Server nos dois serviços;
- criar o Realm da aplicação no Keycloak, com users, clients, roles, groups e scopes;
- versionar configuração base do Realm para execução local opcional;
- atualizar Postman/Insomnia para obter token e chamar as APIs com Bearer Token;
- manter Actuator exposto apenas com endpoints mínimos;
- manter Keycloak fora do core obrigatório do Docker Compose, preferencialmente em profile dedicado de segurança.

Recomendação para produção:

- OAuth2/OIDC com JWT Bearer;
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

Escopos sugeridos:

| Escopo | Endpoints |
|---|---|
| `lancamentos:write` | `POST /api/lancamentos` |
| `lancamentos:read` | `GET /api/lancamentos` |
| `consolidados:read` | `GET /api/consolidados/{data}` |
| `ops:read` | Endpoints operacionais restritos |

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

- documentar OAuth2/OIDC com JWT Bearer e Keycloak como arquitetura alvo;
- registrar a implementação de autenticação/autorização como débito técnico da próxima sprint;
- manter Nginx como camada de entrada;
- usar rate limiting e headers de segurança no Nginx;
- não adicionar Keycloak por padrão no core da POC para não aumentar excessivamente a infraestrutura local.

Essa decisão demonstra maturidade arquitetural: o desenho reconhece o caminho produtivo, mas preserva a simplicidade necessária para avaliação local.
