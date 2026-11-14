# Liva — Lembretes de agendamento pelo WhatsApp

A Liva envia lembretes de atendimentos do SUS pelo WhatsApp, pede ao paciente que confirme a presença, interpreta a resposta (1 = vou, 2 = não vou, 3 = outro horário), atualiza o agendamento e responde ao paciente confirmando o que foi registrado.

O envio e o recebimento de mensagens usam a [Evolution API](https://doc.evolution-api.com), que roda localmente via Docker.

---

## Sumário

1. [Visão geral do fluxo](#visão-geral-do-fluxo)
2. [Arquitetura](#arquitetura)
3. [Pré-requisitos](#pré-requisitos)
4. [Configuração](#configuração)
5. [Como subir tudo](#como-subir-tudo)
6. [Como testar um atendimento](#como-testar-um-atendimento)
7. [Endpoints](#endpoints)
8. [Regras de negócio](#regras-de-negócio)
9. [Testes automatizados](#testes-automatizados)
10. [Solução de problemas](#solução-de-problemas)
11. [Roteiro da apresentação](#roteiro-da-apresentação)
12. [Limitações conhecidas e próximos passos](#limitações-conhecidas-e-próximos-passos)

---

## Visão geral do fluxo

```
 Liva                         Evolution API                  WhatsApp do paciente
  │  POST /message/sendText ──────►│ ─────────────────────────────► "Você consegue ir? 1, 2 ou 3"
  │                                │
  │◄── POST /api/webhooks/evolution│◄───────────────────────────── "1"
  │  (atualiza status: CONFIRMADO) │
  │  POST /message/sendText ──────►│ ─────────────────────────────► "Obrigado! Presença confirmada"
```

1. Um agendamento é criado (`POST /api/agendamentos`) com status `AGENDADO`. O paciente é reaproveitado se já existir, e recebe na hora a mensagem `CONFIRMACAO_AGENDAMENTO`.
2. O lembrete `PEDIDO_CONFIRMACAO` é disparado pela rotina diária (ou na hora, se o atendimento já estiver dentro do prazo). O agendamento passa a `AGUARDANDO_RESPOSTA`.
3. O paciente responde no WhatsApp. A Evolution chama o webhook da Liva.
4. A Liva identifica o agendamento pelo telefone, aplica a resposta (`CONFIRMADO`, `CANCELADO` ou `REAGENDADO`) e envia uma mensagem de retorno.
5. O status pode ser consultado em `GET /api/agendamentos/{id}` e a conversa completa em `GET /api/agendamentos/{id}/mensagens`.

---

## Arquitetura

Arquitetura hexagonal (portas e adaptadores). As dependências sempre apontam para dentro: domínio e aplicação são Java puro, sem nenhum import de `org.springframework`.

```
src/main/java/sus/liva
├── domain/model                      Regras de negócio, Java puro
│   ├── Telefone                        Normaliza para +55 DDD número; chaveComparacao() ignora o 9º dígito
│   ├── agendamento/
│   │   ├── Agendamento                 Entidade; podeReceber(), aguardarResposta(), responder()
│   │   ├── RespostaPaciente            Interpreta "1", "sim", "não vou", "outro horário"...
│   │   ├── StatusAgendamento, TipoAtendimento, TipoLembrete
│   ├── mensagem/RegistroMensagem       Histórico de cada envio
│   └── paciente/                       Paciente, Endereco, Sexo
│
├── application
│   ├── port/in                         Casos de uso expostos (interfaces + DTOs de entrada/saída)
│   │   ├── AgendarAtendimentoUseCase, ConsultarAgendamentoUseCase
│   │   ├── DispararLembreteUseCase, DispararLembretesDoDiaUseCase
│   │   ├── EnviarMensagemUseCase, ProcessarRespostaUseCase
│   │   └── NovoAtendimento, ResultadoDisparo, ResultadoResposta
│   ├── port/out                        O que a aplicação precisa do mundo externo
│   │   ├── AgendamentoRepositoryPort, PacienteRepositoryPort, RegistroMensagemPort
│   │   ├── EnviadorMensagemPort
│   │   └── MensagemSaida, ResultadoEnvio
│   └── usecase                         Implementações (*UseCaseImpl) e ModeloMensagens (textos)
│
└── infrastructure                    Tudo que depende de framework
    ├── adapter/in/web                  Controllers REST, DTOs de request/response, GlobalExceptionHandler
    │   └── teste/                      Endpoints de teste de atendimento (desligáveis)
    ├── adapter/out/persistencia        Postgres via JPA: *Entity (mapeamento), *JpaRepository, *PostgresAdapter (portas)
    ├── adapter/out/whatsapp            EvolutionEnviadorAdapter (real) e ConsoleEnviadorAdapter (fake)
    └── config                          UseCaseConfig (beans), SegurancaConfig, OpenApiConfig

src/main/resources/db/migration       Migrações do Flyway (V1__schema_inicial.sql)
src/main/resources/db/demo            Dados de demonstração (R__dados_demo.sql), aplicados pelo Flyway
src/test/.../adapter/out/memoria      Repositórios em memória, usados como fakes nos testes unitários
```

### Persistência

- Banco **Postgres próprio da Liva** (`liva-postgres` no compose), separado do banco interno da Evolution.
- O schema é criado e versionado pelo **Flyway** (`src/main/resources/db/migration`). Para mudar o schema, crie uma nova migração `V2__descricao.sql`; nunca edite uma migração já aplicada.
- O Hibernate roda com `ddl-auto: validate`: a aplicação não sobe se as entidades e o schema divergirem.
- As entidades JPA (`PacienteEntity`, `AgendamentoEntity`, `RegistroMensagemEntity`) ficam na infraestrutura e são convertidas de/para o domínio (`de(...)` / `paraDominio()`). O domínio não conhece JPA.

| Tabela | Conteúdo |
|---|---|
| `pacientes` | Dados do paciente, endereço (colunas achatadas), cuidador e consentimento. Telefones em E.164 (`+5551993953661`) |
| `agendamentos` | Data/hora, tipo, unidade, preparo e status. FK para `pacientes` |
| `registros_mensagem` | Cada envio de lembrete: destino, `chave_destino` (DDD + 8 últimos dígitos, indexada para casar as respostas do webhook), sucesso, id externo e erro |
| `flyway_schema_history` | Controle de migrações do Flyway |

### Transações

Os casos de uso controlam suas transações pela porta `TransacaoPort` (`executar(() -> ...)`), implementada na infraestrutura por `TransacaoSpringAdapter` (`TransactionTemplate`). Assim a aplicação continua sem Spring e decide explicitamente o que é atômico.

| Caso de uso | Dentro da transação | Fora da transação |
|---|---|---|
| `AgendarAtendimento` | Gravar paciente **e** agendamento (tudo ou nada) | Validações |
| `ProcessarResposta` | Buscar o pedido, ler o agendamento **com trava de linha** (`SELECT ... FOR UPDATE`), aplicar e gravar o novo status | Interpretar o texto; enviar a mensagem de retorno pelo WhatsApp (só depois do commit) |

Regras adotadas:

- **Chamadas externas nunca ficam dentro da transação.** Uma mensagem enviada não pode ser desfeita por rollback, e segurar a transação durante uma chamada HTTP prende conexões do banco. Por isso o retorno ao paciente só é enviado depois que o status foi gravado.
- **Respostas simultâneas são serializadas.** Se o paciente mandar "1" e "2" quase ao mesmo tempo (ou a Evolution reenviar o evento), a trava faz a segunda resposta esperar a primeira e encontrar o agendamento já respondido; só uma é aplicada.
- `buscarAgendamentoParaAtualizar` usa `Propagation.MANDATORY`: falha se for chamado fora de uma transação, evitando trava sem efeito.
- Não usamos Saga: ele serve para operações distribuídas entre serviços com bancos próprios. Aqui é um único serviço e um único banco, então uma transação local resolve.

Convenções do projeto:

- Casos de uso são interfaces em `port/in` implementadas por classes `*UseCaseImpl` em `application/usecase`, registradas como beans em `UseCaseConfig` (sem anotações do Spring na aplicação).
- Validação de entrada acontece no caso de uso (lança `IllegalArgumentException`), e o `GlobalExceptionHandler` converte no formato padrão de erro (ver [Erros](#erros)).
- Controllers usam DTOs próprios (`AgendamentoResponse`, `EnviarMensagemRequest`...) em vez de expor entidades do domínio.
- Sem Lombok e sem comentários no código; a documentação fica neste README e no Swagger.

---

### Fluxo assistencial (triagem → encaminhamento → consulta)

Cada **triagem** abre um ramo independente do histórico do paciente. Encaminhamentos pertencem a uma triagem e as consultas a um encaminhamento. FKs compostas (`triagem_id, paciente_id`) garantem que um ramo nunca misture pacientes.

| Entidade | Estados |
|---|---|
| Triagem | `AGUARDANDO_ATENDIMENTO → EM_ANDAMENTO → FINALIZADA → ENCERRADA` (ou `CANCELADA`); finalizada só recebe retificações |
| Encaminhamento | `PENDENTE → AGUARDANDO_AGENDAMENTO → AGENDADO → EM_ATENDIMENTO → CONCLUIDO` (ou `CANCELADO`); imediato vai direto ao plantão |
| Consulta | `AGENDADO → EM_ANDAMENTO → COMPARECEU`, ou `FALTOU` / `CANCELADO` |

Concorrência:

- `@Version` em paciente, triagem, encaminhamento e consulta; o cliente envia a versão que viu.
- Trava pessimista (`SELECT … FOR UPDATE`) no encaminhamento ao agendar.
- Constraint `EXCLUDE` (`btree_gist`) impede dois horários sobrepostos do mesmo profissional.
- Índices únicos parciais: CPF, Cartão SUS, uma triagem aberta por paciente e a chave de idempotência.
- `lock_timeout` de 5 s.

Violações viram **409** com mensagem amigável.

Toda leitura e alteração clínica gera um registro na tabela `auditoria`.

## Pré-requisitos

- Java 25
- Docker com Docker Compose (no macOS, por exemplo, via Colima: `colima start`)
- Um número de WhatsApp para conectar na Evolution (pode ser o seu)

---

## Configuração

Toda a configuração vem do arquivo `.env` na raiz do projeto. Ele é lido **tanto pelo Docker Compose quanto pela Liva** (via `spring.config.import`), então não é preciso exportar variáveis. O `.env` está no `.gitignore`; use o `.env.example` como modelo.

| Variável | Usada por | Descrição |
|---|---|---|
| `API_KEY` | Liva | Chave do header `X-API-Key` para as rotas de sistema (`/api/agendamentos`, `/api/mensagens`, `/api/disparos`, `/api/teste`). **Obrigatória**: sem ela a Liva não sobe. Gere com `openssl rand -hex 32` |
| `JWT_SEGREDO` | Liva | Segredo HS256 dos tokens de sessão (mínimo 32 caracteres). **Obrigatório**. Gere com `openssl rand -hex 32` |
| `FLYWAY_LOCATIONS` | Liva | Pastas de migração. Padrão `classpath:db/migration,classpath:db/demo`; use só `classpath:db/migration` para subir sem dados de demonstração |
| `CORS_ORIGENS_PERMITIDAS` | Liva | Origens do front-end que podem chamar a API pelo navegador, separadas por vírgula. Padrão `http://localhost:3000` |
| `WHATSAPP_PROVEDOR` | Liva | `evolution` envia de verdade; `console` só escreve a mensagem no log (padrão) |
| `EVOLUTION_URL` | Liva | URL da Evolution. Padrão `http://localhost:8080` |
| `EVOLUTION_API_KEY` | Liva e Evolution | Chave de autenticação da Evolution (gere com `openssl rand -hex 32`) |
| `EVOLUTION_INSTANCIA` | Liva | Nome da instância do WhatsApp na Evolution. Padrão `liva` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | Evolution | Banco interno da Evolution |
| `LIVA_DB_HOST` / `LIVA_DB_PORT` | Liva | Onde está o Postgres da Liva. Padrão `localhost:5433` |
| `LIVA_DB_NAME` / `LIVA_DB_USER` | Liva e compose | Nome do banco e usuário. Padrão `liva` / `liva` |
| `LIVA_DB_PASSWORD` | Liva e compose | Senha do banco da Liva (gere com `openssl rand -hex 24`) |
| `WEBHOOK_TOKEN` | Liva | Token exigido em `?token=` no webhook (gere com `openssl rand -hex 24`) |
| `WEBHOOK_URL` | Configuração do webhook | URL que a Evolution chama: `http://host.docker.internal:8081/api/webhooks/evolution` |
| `WEBHOOK_ACEITAR_MENSAGENS_PROPRIAS` | Liva | `true` processa mensagens enviadas pelo próprio número conectado (necessário para testar respondendo a si mesmo). **Use `false` em produção** |
| `ATENDIMENTO_TESTE_HABILITADO` | Liva | `false` desliga os endpoints `/api/teste/**`. Padrão `true` |
| `FUSO_HORARIO` | Liva | Fuso usado para saber qual é "hoje" e para o horário do agendador. Padrão `America/Sao_Paulo` |
| `DISPARO_DIARIO_HABILITADO` | Liva | `false` desliga o disparo automático. Padrão `true` |
| `DISPARO_DIARIO_CRON` | Liva | Quando a rotina diária roda (cron do Spring, com segundos). Padrão `0 0 8 * * *` (todo dia às 08:00). `-` desliga |
| `LEMBRETE_PEDIDO_DIAS_ANTES` | Liva | Quantos dias antes da consulta sai o pedido de confirmação. Padrão `3` |
| `LEMBRETE_VESPERA_DIAS_ANTES` | Liva | Quantos dias antes sai o lembrete da véspera. Padrão `1` |
| `FILA_AUTOMACAO_HABILITADA` | Liva | `false` desliga o ciclo automático da fila (ofertas, prazos e aviso final). Padrão `true` |
| `FILA_OFERTA_IMEDIATA` | Liva | Depois de um aceite, recusa ou retomada, oferece a próxima vaga na hora (envia WhatsApp). Padrão: o mesmo valor de `FILA_AUTOMACAO_HABILITADA` |
| `FILA_INTERVALO` | Liva | Intervalo entre ciclos da fila (ISO-8601). Padrão `PT1M` |
| `FILA_PRAZO_CONFIRMACAO` | Liva | Até quanto tempo antes da consulta a pessoa deve confirmar. Padrão `PT24H` |
| `FILA_AVISO_FINAL` | Liva | Quanto tempo antes do fim do prazo sai o aviso final para quem não respondeu. Padrão `PT6H` |
| `FILA_SEM_RESPOSTA` | Liva | O que fazer quando o prazo vence sem resposta: `MANTER` (padrão, a consulta continua) ou `LIBERAR` (cancela e oferece a vaga) |
| `FILA_EXIGIR_ENTREGA` | Liva | Com `LIBERAR`, só libera se a Evolution confirmou a entrega do pedido. Padrão `true` |
| `FILA_PRAZO_OFERTA` | Liva | Tempo para responder a uma vaga oferecida. Padrão `PT2H` |
| `FILA_ANTECEDENCIA_MINIMA` | Liva | Não oferece vagas que começam antes disso; a oferta expira nesse limite. Padrão `PT3H` |
| `FILA_OFERTAS_POR_VAGA` | Liva | Máximo de pessoas a quem a mesma vaga é oferecida, uma de cada vez. Padrão `5` |
| `FILA_ESPERA_SEM_CANDIDATO` | Liva | Quanto esperar para tentar de novo uma vaga sem ninguém elegível. Padrão `PT10M` |

Portas:

| Porta | Serviço |
|---|---|
| `8080` | Evolution API (e o painel em `/manager`) |
| `8081` | Liva (API e Swagger) |
| `5433` | Postgres da Liva (`liva-postgres`) |
| — | Postgres e Redis da Evolution ficam só na rede interna do Docker |

---

## Como subir tudo

### 1. Containers (Evolution API e Postgres da Liva)

```bash
docker compose up -d
docker compose ps           # evolution-api, evolution-postgres, evolution-redis e liva-postgres
curl localhost:8080/        # deve responder "Welcome to the Evolution API"
```

Para acessar o banco da Liva:

```bash
source .env
docker exec -it liva-postgres psql -U $LIVA_DB_USER -d $LIVA_DB_NAME
```

### 2. Instância do WhatsApp

A integração **precisa** ser `WHATSAPP-BAILEYS`. (A opção `EVOLUTION` / "Evolution Channel" não conecta ao WhatsApp: as mensagens ficam só dentro da Evolution.)

```bash
source .env
curl -X POST localhost:8080/instance/create \
  -H "apikey: $EVOLUTION_API_KEY" -H "Content-Type: application/json" \
  -d "{\"instanceName\": \"$EVOLUTION_INSTANCIA\", \"integration\": \"WHATSAPP-BAILEYS\", \"qrcode\": true}"
```

Depois conecte o celular: abra http://localhost:8080/manager, entre com a `EVOLUTION_API_KEY`, abra a instância e leia o QR code em **WhatsApp → Configurações → Aparelhos conectados → Conectar um aparelho**.

Confirme a conexão:

```bash
curl -H "apikey: $EVOLUTION_API_KEY" localhost:8080/instance/connectionState/$EVOLUTION_INSTANCIA
# {"instance":{"instanceName":"liva","state":"open"}}
```

### 3. Webhook (respostas dos pacientes)

```bash
source .env
curl -X POST localhost:8080/webhook/set/$EVOLUTION_INSTANCIA \
  -H "apikey: $EVOLUTION_API_KEY" -H "Content-Type: application/json" \
  -d "{\"webhook\": {\"enabled\": true, \"url\": \"$WEBHOOK_URL?token=$WEBHOOK_TOKEN\", \"byEvents\": false, \"base64\": false, \"events\": [\"MESSAGES_UPSERT\", \"MESSAGES_UPDATE\"]}}"
```

`MESSAGES_UPSERT` traz as respostas dos pacientes. `MESSAGES_UPDATE` traz a confirmação de entrega (`DELIVERY_ACK`/`READ`), usada pela fila para não tratar falha de entrega como falta de resposta.

O `docker-compose.yml` já mapeia `host.docker.internal` para o host, então o container alcança a Liva na porta 8081.

### 4. Liva

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html (clique em **Authorize** e cole a `API_KEY` do `.env`)

Todas as chamadas a `/api/**` precisam do header `X-API-Key`. Para os exemplos abaixo, carregue a chave no terminal:

```bash
export API_KEY=$(grep '^API_KEY=' .env | cut -d= -f2)
```

Ao subir, o Flyway aplica as migrações pendentes e o `db/demo/R__dados_demo.sql`, que carrega os dados de demonstração (veja [Usuários de demonstração](#usuários-de-demonstração)). Os inserts são idempotentes: registros que já existem são mantidos como estão. Os testes automatizados usam só `db/migration`.

---

## Como testar um atendimento

O jeito mais simples é pelo Swagger, na seção **Teste de atendimento**.

### Agendamento real

```bash
curl -X POST localhost:8081/api/agendamentos -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"nomePaciente": "Maria da Silva", "cpf": "52998224725", "telefone": "51993953661",
       "dataHora": "2026-11-20T09:30:00", "tipo": "CONSULTA", "unidade": "UBS Centro",
       "endereco": "Rua das Flores, 123 - Centro", "preparo": "Jejum de 8 horas"}'
```

Obrigatórios: `nomePaciente`, `telefone`, `dataHora` (no futuro), `tipo`, `unidade` e `endereco`. Opcionais: `cpf`, `nomeCuidador` + `telefoneCuidador`, `preparo`.

A resposta (`201`, com `Location`) traz `pacienteId`, `pacienteNovo`, o `agendamento` e o resultado de cada envio em `confirmacao` e `pedidoConfirmacao` (este último só quando o atendimento já está dentro do prazo do pedido). Faça o mesmo pedido de novo com outra data e veja `pacienteNovo: false`.

### Teste completo pelo WhatsApp

1. **Crie o atendimento** para o número que vai receber a mensagem:

   ```bash
   curl -X POST localhost:8081/api/teste/atendimentos -H "X-API-Key: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"telefone": "51993953661", "nomePaciente": "Paciente Teste", "preparo": "Jejum de 8 horas"}'
   ```

   Só `telefone` é obrigatório. Os demais campos têm padrão (data: daqui a 2 dias às 09:30; unidade: UBS Centro). Como a data está dentro do prazo, chegam duas mensagens: a confirmação do agendamento e o pedido "1, 2 ou 3". A resposta traz o `agendamento.id`, o resultado de cada envio e o `proximoPasso`. Usando o mesmo telefone e nome, o paciente de teste é reaproveitado.

2. **Responda no WhatsApp** com `1`, `2` ou `3`. Você recebe a mensagem de retorno.

3. **Consulte o status**:

   ```bash
   curl localhost:8081/api/agendamentos/<agendamento.id> -H "X-API-Key: $API_KEY"
   ```

### Variações úteis

| Quero testar... | Corpo do `POST /api/teste/atendimentos` |
|---|---|
| Paciente com cuidador (os dois recebem o lembrete) | `{"telefone": "...", "nomeCuidador": "João", "telefoneCuidador": "..."}` |
| Lembrete de véspera com preparo | `{"telefone": "...", "preparo": "Jejum de 8 horas", "lembrete": "LEMBRETE_VESPERA"}` |
| Só a confirmação, sem o pedido "1, 2 ou 3" | `{"telefone": "...", "dataHora": "<daqui a mais de 3 dias>"}` |

Campos opcionais devem ser **omitidos** (não envie `"string"`, que é o valor de exemplo do Swagger).

### Simular a resposta sem usar o celular

```bash
curl -X POST localhost:8081/api/teste/atendimentos/respostas -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"telefone": "51993953661", "texto": "1"}'
```

Equivale à mensagem chegando pelo webhook. Se for aplicada, a mensagem de retorno é enviada de verdade ao paciente.

### Testando com o seu próprio número

Se o número do paciente for o mesmo conectado na Evolution, as mensagens aparecem no chat **"Você"** (mensagens para mim mesmo), sem notificação. Para que a sua resposta nesse chat seja processada, mantenha `WEBHOOK_ACEITAR_MENSAGENS_PROPRIAS=true` no `.env`.

### Sem WhatsApp (modo console)

Com `WHATSAPP_PROVEDOR=console`, nada é enviado: as mensagens aparecem no log da Liva com o prefixo `[WHATSAPP FAKE]`. Use o endpoint de simular resposta para completar o fluxo.

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/teste/atendimentos` | Igual ao agendamento real, com valores padrão; `lembrete` envia mais um lembrete na hora (201) |
| `POST` | `/api/teste/atendimentos/respostas` | Simula a resposta do paciente (`{"telefone", "texto"}`) |
| `POST` | `/api/agendamentos` | Agenda um atendimento, reaproveita o paciente e envia a confirmação (201; 400 inválido) |
| `GET` | `/api/agendamentos?inicio=&fim=&status=` | Lista para a equipe: agendamentos do período (padrão: hoje; máx. 31 dias) com paciente, telefone e contagem `porStatus`. `status` pode repetir |
| `GET` | `/api/agendamentos/{id}` | Consulta o agendamento e o status atual (404 se não existir) |
| `GET` | `/api/agendamentos/{id}/mensagens` | Histórico em ordem cronológica: mensagens enviadas (`ENVIADA`) e respostas do paciente (`RECEBIDA`, com o texto original e a interpretação) |
| `POST` | `/api/agendamentos/{id}/disparos?tipo=...&forcar=true` | Dispara um lembrete de um agendamento. `forcar=false` não reenvia o que já foi enviado |
| `POST` | `/api/disparos/executar?data=AAAA-MM-DD` | Executa agora a rotina diária para a data informada (padrão: hoje). Mesma lógica do agendador |
| `POST` | `/api/mensagens` | Envia uma mensagem avulsa `{"telefone", "texto"}` (200 enviado, 502 falha na Evolution, 400 inválido) |
| `POST` | `/api/webhooks/evolution?token=...` | Chamado pela Evolution. 401 sem token válido; sempre 200 com o resultado do processamento |

Também há `GET /actuator/health` (sem chave) para conferir se a Liva e o banco estão de pé.

Fluxo assistencial (exige `Authorization: Bearer`; a permissão por papel está em [Segurança](#segurança)):

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/entrar` | `{"login", "senha"}` → token, validade e usuário |
| `POST` | `/api/auth/portal/codigo` | `{"documento", "dataNascimento"}`: envia o código do portal pelo WhatsApp (sempre 202) |
| `POST` | `/api/auth/portal/ativar` | `{"documento", "dataNascimento", "codigo", "senha"}` → token do paciente |
| `GET` | `/api/auth/eu` | Usuário da sessão |
| `POST` / `GET` | `/api/pacientes` · `?termo=` | Cadastro (409 se CPF/CNS já existir) e pesquisa por nome, CPF ou CNS |
| `GET` / `PUT` | `/api/pacientes/{id}` | Cadastro; o `PUT` envia `versao` (409 se outra pessoa alterou) |
| `GET` | `/api/pacientes/{id}/historico` | Um ramo por triagem; dados clínicos só para quem participa |
| `POST` / `GET` | `/api/triagens` · `/api/triagens/fila` | Chegada do paciente e fila |
| `GET` | `/api/triagens/{id}` | Triagem com encaminhamentos, consultas, retificações e evoluções |
| `POST` | `/api/triagens/{id}/iniciar` · `/finalizar` · `/cancelar` · `/encerrar` · `/retificacoes` · `/encaminhamentos` | Ciclo da triagem |
| `PUT` | `/api/triagens/{id}/avaliacao` | Queixa, sinais vitais e prioridade (com `versao`) |
| `GET` | `/api/encaminhamentos/recebidos` | Encaminhamentos da especialidade do especialista |
| `POST` | `/api/encaminhamentos/{id}/receber` · `/retorno` · `/concluir` · `/cancelar` | Plantão, retorno, alta e cancelamento |
| `GET` | `/api/encaminhamentos/{id}/horarios?inicio=&fim=` | Horários livres (até 31 dias) |
| `POST` | `/api/encaminhamentos/{id}/consultas` | Agenda a consulta; header `Idempotency-Key` opcional (409 se o horário acabou de ser reservado) |
| `GET` | `/api/consultas/agenda?inicio=&fim=` | Agenda do especialista |
| `POST` | `/api/consultas/{id}/iniciar` · `/finalizar` · `/evolucoes` · `/falta` · `/remarcar` · `/cancelar` | Atendimento e mudanças na consulta (com `versao`) |
| `GET` | `/api/portal/historico` | Histórico do paciente logado |
| `GET` | `/api/catalogo/especialidades` · `/api/catalogo/especialistas?especialidade=` | Catálogo para os encaminhamentos |
| `GET` | `/api/portal/fila` | Posição do paciente em cada fila e as vagas oferecidas a ele |
| `POST` | `/api/portal/fila/ofertas/{id}/aceitar` · `/recusar` | Resposta do paciente à vaga (409 se a vaga não estiver mais disponível) |
| `GET` | `/api/filas?especialidade=&profissional=&unidade=&pagina=&tamanho=` | Painel: indicadores, fila paginada, vagas, ofertas, sem confirmação e movimentações |
| `GET` | `/api/filas/entradas/{id}` | Posição, ofertas e histórico de uma entrada |
| `POST` | `/api/filas/entradas/{id}/pausar` · `/retomar` | Intervenção auditada da equipe (`{"motivo"}` para pausar) |
| `POST` | `/api/filas/ofertas/{id}/aceitar` · `/recusar` | A equipe registra a resposta dada por telefone ou na unidade |
| `POST` | `/api/filas/consultas/{id}/confirmar-presenca` | Confirmação de presença assistida pela recepção |
| `POST` | `/api/filas/processar` | Executa agora o ciclo da fila |
| `GET` | `/api/encaminhamentos/{id}/agenda` | `{"possuiAgenda"}`: se algum especialista da especialidade tem agenda aberta. Sem agenda, a tela de horários avisa em vez de mostrar semanas vazias |
| `GET` | `/api/faltas?semanas=12` | Prevenção de faltas: evolução semanal (só semanas completas), motivos dos últimos 90 dias, faltas por especialidade e consultas dos próximos 7 dias ordenadas pelo risco de falta |
| `POST` | `/api/consultas/{id}/ligacao` | Resultado da ligação de prevenção (`{"resultado"}`: `CONFIRMOU`, que também confirma a presença, `VAI_REMARCAR` ou `NAO_ATENDEU`, que soma 10 pontos ao risco) |
| `POST` | `/api/consultas/{id}/motivo-de-ausencia` | Registra o motivo de uma falta ou cancelamento (`{"motivo"}`: `ESQUECI`, `TRANSPORTE`, `TRABALHO`, `SAUDE`, `NAO_PRECISA`, `OUTRO`) pelo portal ou pela equipe |

### Erros

Todo erro, de qualquer rota, segue o mesmo formato:

```json
{
  "timestamp": "2026-11-14T03:40:12.345Z",
  "status": 400,
  "codigo": "REQUISICAO_INVALIDA",
  "mensagem": "CPF inválido: \"123.456.789-00\"",
  "caminho": "/api/agendamentos"
}
```

| `codigo` | HTTP | Quando |
|---|---|---|
| `REQUISICAO_INVALIDA` | 400 | Validação do caso de uso, JSON malformado, enum/data/UUID inválido, parâmetro obrigatório ausente |
| `NAO_AUTORIZADO` | 401 | Sem `X-API-Key` (ou chave errada); token do webhook inválido |
| `NAO_ENCONTRADO` | 404 | Agendamento inexistente ou rota inexistente |
| `METODO_NAO_PERMITIDO` | 405 | Método HTTP não aceito na rota |
| `CONFLITO` | 409 | Estado não permite a operação ou violação de unicidade (ex.: CPF duplicado) |
| `TIPO_DE_CONTEUDO_NAO_SUPORTADO` | 415 | Corpo sem `Content-Type: application/json` |
| `ERRO_INTERNO` | 500 | Erro inesperado (detalhes só no log) |
| `FALHA_WHATSAPP` | 502 | A Evolution não conseguiu enviar a mensagem avulsa |
| `SERVICO_INDISPONIVEL` | 503 | Banco fora do ar |

Quem monta a resposta: `GlobalExceptionHandler` (erros dentro dos controllers), `ErroPadraoController` (erros que não chegam a um controller, no `/error`) e `ChaveApiFiltro` (401). O formato é o record `ErroResposta` e os códigos estão em `CodigoErro` (`adapter/in/web/erro`).

### Segurança

- **Spring Security, sem sessão no servidor.** As pessoas entram em `POST /api/auth/entrar` e recebem um JWT (HS256, `JWT_SEGREDO`).
  - O token traz o usuário, o papel e o vínculo com profissional ou paciente.
  - Ele vale 8 h para a equipe e 2 h para o paciente.
  - Rotas do fluxo assistencial exigem `Authorization: Bearer <token>`. Sem ele, `401`.
- **Papéis:** `ADMINISTRADOR`, `RECEPCAO`, `CLINICO_GERAL`, `ESPECIALISTA` e `PACIENTE`.
  - A regra fina de cada operação fica nos casos de uso, via `Ator`, e a negação é `403`.
  - Exemplos: o especialista só lê ramos em que atua, e o paciente só vê o que é dele.
- **Senhas:** BCrypt. Após 5 senhas erradas, o acesso fica bloqueado por 15 min.
- **Portal do paciente:**
  - Liberado só após a primeira triagem concluída.
  - Primeiro acesso: `POST /api/auth/portal/codigo` com CPF/CNS **e** data de nascimento. Um código de 6 dígitos vai pelo WhatsApp cadastrado, vale 10 min e aceita 5 tentativas.
  - A resposta é sempre a mesma, para não revelar quem é paciente.
  - Com o código, `POST /api/auth/portal/ativar` define a senha. O mesmo fluxo serve para "esqueci a senha".
  - Depois disso, o paciente entra com o CPF/CNS e a senha.
- **A `API_KEY` vale só para as rotas de sistema** (`/api/agendamentos`, `/api/mensagens`, `/api/disparos`, `/api/teste`): scripts, `curl` e Swagger.
  - Essas rotas também aceitam o token de `ADMINISTRADOR` ou `RECEPCAO`.
  - A chave não abre nenhuma rota clínica.
- **Webhook:** `/api/webhooks/**` é protegido pelo `?token=WEBHOOK_TOKEN`.
- **Front (BFF):**
  - O navegador nunca vê o token: o Next guarda o token no cookie httpOnly `liva_sessao` (`SameSite=Lax`, `Secure` em produção).
  - O proxy `/api/liva/**` repassa o token como `Bearer`.
  - O proxy recusa escrita vinda de outra origem e não expõe `/api/auth/**` nem `/api/webhooks/**`.
  - `src/proxy.ts` manda quem está sem sessão para `/entrar`.
- **Auditoria:** login, ativação do portal e toda leitura ou alteração clínica são gravados na tabela `auditoria`.
- **CORS:** só as origens de `CORS_ORIGENS_PERMITIDAS` são aceitas. Na prática, o front chama a API pelo servidor do Next.

#### Usuários de demonstração

Criados pelo `db/demo/R__dados_demo.sql`. As senhas estão gravadas só como hash BCrypt; a senha de demonstração é combinada com a equipe e não fica no repositório.

| Login | Papel | O que já aparece |
|---|---|---|
| `admin@liva.demo` | Administração | Fila, pacientes e agendamentos |
| `recepcao@liva.demo` | Recepção | Fila com 3 pacientes, 8 pacientes cadastrados |
| `clinico@liva.demo` | Clínico geral (Dr. Paulo Mendes) | João e Ana aguardando; Pedro em triagem com ele |
| `ortopedia@liva.demo` | Especialista em Ortopedia (Dra. Ana Ribeiro) | Roberto Santos confirmado para hoje às 14:00 |
| `cardiologia@liva.demo` | Especialista em Cardiologia (Dr. Carlos Lima) | Francisco Lima em consulta agora; Carla Nunes em 3 dias; Maria aguardando agendamento |
| `plantao@liva.demo` | Plantão / pronto atendimento (Dra. Júlia Costa) | Luiza Martins, emergência, aguardando recebimento |
| `neurologia@liva.demo` | Especialista em Neurologia (Dr. Rafael Souza) | Agenda de terça a quinta, pronta para receber encaminhamentos |
| `dermatologia@liva.demo` | Especialista em Dermatologia (Dra. Mariana Alves) | Agenda segunda, quarta e sexta |
| `ginecologia@liva.demo` | Especialista em Ginecologia (Dra. Helena Prado) | Agenda segunda, terça e quinta |
| `pediatria@liva.demo` | Especialista em Pediatria (Dr. Tiago Martins) | Agenda segunda, quarta e sexta |
| CPF `158.813.998-03` | Paciente Rosa Oliveira, criada por `scripts/preparar-apresentacao.sh` | Duas faltas, risco alto em "Ligar primeiro" e uma consulta de hoje para registrar a falta |
| `12345678909` | Paciente (Maria da Silva) | Um atendimento encerrado com ortopedia e um com cardiologia liberado para agendar no portal |

As datas são relativas ao dia em que o banco foi criado. Todos os pacientes de demonstração usam o mesmo telefone, então lembretes automáticos só vão para esse número.

Ortopedia e Cardiologia atendem de segunda a sexta, das 08:00 às 12:00 e das 13:30 às 17:00, em horários de 30 min.

---

## Regras de negócio

### Lembretes (`TipoLembrete`)

| Tipo | Quando (rotina do dia) | Conteúdo |
|---|---|---|
| `CONFIRMACAO_AGENDAMENTO` | Ao criar o agendamento | Data, hora, endereço, documentos |
| `PEDIDO_CONFIRMACAO` | `LEMBRETE_PEDIDO_DIAS_ANTES` (padrão 3 dias antes) | Pergunta "Você consegue ir? 1, 2 ou 3". Muda o status para `AGUARDANDO_RESPOSTA` |
| `AVISO_FINAL_CONFIRMACAO` | `FILA_AVISO_FINAL` antes do fim do prazo de confirmação | Último aviso para quem ainda não respondeu ao pedido (ciclo da fila) |
| `LEMBRETE_VESPERA` | `LEMBRETE_VESPERA_DIAS_ANTES` (padrão 1 dia antes) | Data, hora, endereço e preparo |

A antecedência do pedido e da véspera vem de `AntecedenciaDosLembretes` (variáveis acima); o padrão continua em `TipoLembrete.getDiasAntes()`.

### Agendamento e cadastro do paciente

- **Com CPF**: se já existe paciente com esse CPF, ele é reaproveitado e o telefone é atualizado.
- **Sem CPF (ou CPF ainda não cadastrado)**: procura pelo telefone **e** nome (ignorando maiúsculas, acentos e espaços). Telefone sozinho não basta, porque famílias costumam usar o mesmo número para vários pacientes. Se o paciente encontrado não tinha CPF, o CPF informado é gravado.
- Se nada for encontrado, um paciente novo é criado. O CPF é único no banco (`V2__busca_de_paciente.sql`).
- Se `telefoneCuidador` for informado, o cuidador do paciente é atualizado; caso contrário, o cuidador atual é mantido.
- O CPF é validado pelos dígitos verificadores e pode ser enviado com ou sem pontuação.
- Paciente e agendamento são gravados numa única transação. As mensagens só são enviadas **depois** do commit; se o WhatsApp falhar, o agendamento continua válido e o erro aparece em `confirmacao`.
- Se o atendimento já está dentro do prazo do pedido de confirmação (hoje + 3 dias ou antes), o pedido é enviado na hora, já que a rotina diária não pegaria mais esse dia. O `jaEnviado` impede que a rotina repita o envio.

### Disparo automático diário

O `DisparoDiarioAgendador` (`adapter/in/agendador`) roda no horário de `DISPARO_DIARIO_CRON`, no fuso `FUSO_HORARIO`, e chama `DispararLembretesDoDiaUseCase`:

1. Busca os agendamentos de hoje + `diasAntes` de `PEDIDO_CONFIRMACAO` e envia o pedido de confirmação.
2. Busca os agendamentos de hoje + `diasAntes` de `LEMBRETE_VESPERA` e envia o lembrete da véspera.

Regras:

- **Idempotente**: o que já foi enviado com sucesso não é reenviado (`jaEnviado`). Rodar várias vezes no mesmo dia é seguro.
- **Falhas são reenviadas na próxima execução**: um envio que falhou (ex.: Evolution fora do ar) não conta como enviado.
- **Um agendamento com problema não interrompe os outros**: o erro vai para o log e a rotina segue.
- Agendamentos `CANCELADO`, `REAGENDADO` etc. e pacientes sem consentimento são ignorados (`Agendamento.podeReceber`). `CONFIRMADO` ainda recebe o lembrete da véspera, que leva o preparo.
- O "hoje" é calculado no fuso configurado, não no fuso do servidor.

Ao final de cada execução aparece no log:

```
Disparo diário de 2026-11-14: 3 mensagem(ns) enviada(s)
```

**Dica para a apresentação**: para não esperar até as 08:00, use `DISPARO_DIARIO_CRON=0 * * * * *` no `.env` (roda a cada minuto) e reinicie a Liva; como a rotina é idempotente, cada lembrete sai uma única vez. Outra opção é chamar `POST /api/disparos/executar` pelo Swagger. Para um agendamento ser pego pela rotina, a data dele precisa ser exatamente hoje + 3 dias (pedido) ou hoje + 1 dia (véspera); o endpoint de teste aceita `dataHora`.

Um lembrete só é enviado se o paciente tiver consentimento e o status permitir (`AGENDADO` ou `AGUARDANDO_RESPOSTA`; `CONFIRMADO` recebe apenas o `LEMBRETE_VESPERA`). Se o paciente tem cuidador, o cuidador recebe uma cópia.

### Respostas do paciente (`RespostaPaciente`)

Acentos, maiúsculas e pontuação são ignorados. O primeiro número da mensagem também vale ("1 - Sim, vou").

| Resposta | Também aceita | Novo status | Retorno enviado |
|---|---|---|---|
| `1` | sim, vou, sim vou, confirmo | `CONFIRMADO` | "Sua presença está confirmada…" |
| `2` | não, não vou, não vou conseguir, cancelar | `CANCELADO` | "Cancelamos seu atendimento… a vaga será liberada" |
| `3` | outro horário, quero outro horário, remarcar | `REAGENDADO` | "A equipe vai entrar em contato para remarcar" |

A resposta só é aplicada se o agendamento estiver `AGUARDANDO_RESPOSTA`; respostas seguintes são ignoradas. O agendamento é encontrado pelo **último `PEDIDO_CONFIRMACAO` enviado com sucesso** para aquele telefone.

**Resposta não reconhecida** (ex.: "talvez"): se há um pedido aguardando resposta, o paciente recebe "Desculpe, não entendi sua resposta… Responda apenas com o número: 1, 2 ou 3". A orientação não se repete em menos de 5 minutos. Sem pedido pendente, a mensagem é ignorada em silêncio.

**Histórico**: toda mensagem enviada (lembretes, retornos, orientações e avulsas) é gravada em `registros_mensagem` com o `id` devolvido pela Evolution. Quando a Evolution devolve no webhook uma mensagem que a própria Liva enviou (acontece ao testar com o próprio número), ela é reconhecida pelo `id` e ignorada.

### Fila de espera e reaproveitamento de vagas

A fila não é um sistema à parte: ela nasce dos encaminhamentos que já existem.

- **Uma entrada por necessidade**: cada encaminhamento eletivo de uma triagem finalizada (`AGUARDANDO_AGENDAMENTO` ou `AGENDADO`) gera uma entrada em `entradas_fila`, única por `(encaminhamento, ciclo)`. Duas triagens da mesma pessoa são duas filas independentes; um retorno abre um novo ciclo.
- **Ordem**: prioridade clínica da triagem, depois data de entrada, depois id. A posição considera quem disputa a mesma especialidade e o mesmo profissional (ou qualquer um).
- **Vagas liberadas**: consultas futuras `CANCELADO`/`REAGENDADO` cujo horário continua livre viram `vagas_liberadas` (origem cancelamento, sem confirmação ou antecipação). A agenda (`agendamentos` com a constraint `EXCLUDE`) continua sendo a fonte da verdade da capacidade.
- **Oferta**: o ciclo oferece cada vaga à próxima pessoa elegível, uma de cada vez, pelo WhatsApp (com link de acesso ao portal) e no portal. Quem liberou o horário não o recebe de volta. Enquanto a oferta está ativa, o horário fica reservado e some dos horários livres.
- **Aceitar**: em uma transação curta, trava oferta → vaga → entrada → consulta atual → encaminhamento, cria a nova consulta já `CONFIRMADO`, cancela a antiga (que vira uma nova vaga, em cascata) e marca a vaga como reaproveitada. Aceite repetido devolve a mesma consulta.
- **Recusar ou deixar expirar** não tira ninguém da fila nem muda a posição; a vaga segue para a próxima pessoa. A mesma vaga nunca é oferecida duas vezes à mesma entrada e para depois de `FILA_OFERTAS_POR_VAGA` tentativas.
- **Pelo WhatsApp**: "ACEITO", "aceitar", "quero a vaga" aceitam; "NÃO POSSO", "recuso" recusam. São palavras diferentes das respostas 1/2/3 do pedido de confirmação.
- **Silêncio não é cancelamento**: com `FILA_SEM_RESPOSTA=MANTER` a consulta continua e o painel mostra "mantida sem resposta" (ou "sem contato" se a mensagem não foi entregue). Com `LIBERAR`, só libera quando a entrega foi confirmada; a pessoa volta para a fila com a data de entrada original.
- **Concorrência**: `SELECT ... FOR UPDATE` onde há disputa, `FOR UPDATE SKIP LOCKED` nos passos do ciclo, índices únicos parciais (uma oferta ativa por vaga e por entrada; uma vaga aberta por horário) e nenhuma chamada ao WhatsApp dentro de transação. Os passos guardam o estado no banco, então o ciclo pode rodar em mais de uma instância e retoma ofertas não enviadas depois de reiniciar.
- **Automação desligada**: com `FILA_AUTOMACAO_HABILITADA=false` nada é enviado sozinho. O painel `/filas` ainda atualiza a fila, registra vagas e expira ofertas vencidas (só no banco) toda vez que é aberto; ofertas novas só saem pelo botão "Processar agora".
- **Telas**: `/portal/fila` (posição por encaminhamento e a oferta com contagem regressiva) e `/filas` (indicadores, fila paginada, vagas, ofertas, consultas sem confirmação, movimentações e intervenções auditadas). O especialista vê só a própria especialidade.

### Prevenção de faltas

- **Risco de falta** (`RiscoDeFalta`): cada consulta dos próximos 7 dias recebe de 0 a 100 pontos. Cada falta nos últimos 12 meses soma 25 (até 2), WhatsApp não entregue soma 25, pedido de confirmação sem resposta soma 20, consulta marcada há mais de 30 dias soma 15, 2 ou mais cancelamentos em 90 dias somam 10 e primeira consulta soma 5. Quem compareceu 3 vezes ou mais sem faltar perde 10, e quem confirmou presença perde 40. Alto a partir de 50, médio a partir de 25.
- **Motivo da ausência**: ao registrar uma falta, a Liva manda pelo WhatsApp uma pergunta com 6 opções e um link para remarcar. Depois de um cancelamento pelo WhatsApp (opção 2), pergunta o motivo também. O paciente responde pelo número ou por escrito (“sem dinheiro pro ônibus” vira Transporte). Texto não reconhecido reenvia as opções, no máximo a cada 5 minutos, e “MENU” volta ao assistente. No portal, o motivo é opcional ao cancelar.
- **Como chegar e agenda do celular**: a confirmação do agendamento e o lembrete da véspera trazem um link do Google Maps com o endereço da unidade. A confirmação e o cartão da consulta no portal também trazem um link para salvar a consulta na agenda do Google.
- **Ligação de prevenção**: na lista “Ligar primeiro”, a equipe registra se a pessoa confirmou, vai remarcar ou não atendeu. Confirmar pela ligação confirma a presença; não atender soma 10 pontos ao risco.
- **Impacto**: o painel estima quantas consultas deixaram de ficar vazias nas últimas 4 semanas, somando as faltas a menos (comparadas ao ritmo do início do período) às vagas reaproveitadas pela fila.
- A tela **Prevenção de faltas** (`/faltas`, recepção e administração) mostra a lista “Ligar primeiro”, a evolução semanal, os motivos com uma ação sugerida e as faltas por especialidade. O seed `R__faltas_demo.sql` cria 12 semanas de histórico para a demonstração.

### Telefones

`Telefone.de(...)` aceita `(11) 98765-4321`, `11987654321` ou `5511987654321` e normaliza para `+5511987654321`. O WhatsApp às vezes identifica números brasileiros **sem o 9º dígito** (ex.: `555193953661@s.whatsapp.net`); por isso a comparação usa `chaveComparacao()` (DDD + 8 últimos dígitos). O webhook também entende remetentes no formato `@lid`, usando `remoteJidAlt`.

---

## Testes automatizados

```bash
./mvnw test
```

Os testes de persistência e o `LivaApplicationTests` sobem um Postgres temporário com **Testcontainers**, então o Docker precisa estar rodando (`colima start`). Eles não usam o `liva-postgres` do compose. Os testes unitários usam os repositórios em memória de `src/test` como fakes.

| Classe | Cobre |
|---|---|
| `TelefoneTest` | Formatos válidos/inválidos, `apenasDigitos()` |
| `RespostaPacienteTest` | Interpretação das respostas |
| `EnviarMensagemUseCaseImplTest` | Envio avulso com fake da porta de saída |
| `AgendarAtendimentoUseCaseImplTest` | Criação, reaproveitamento por CPF e por telefone + nome, família no mesmo telefone, CPF inválido, envio da confirmação e do pedido depois do commit, falha do WhatsApp |
| `ProcessarRespostaUseCaseImplTest` | Aplicação da resposta, 9º dígito, retorno ao paciente, orientação para resposta não reconhecida (sem repetir em 5 min), eco da própria Liva, histórico |
| `EvolutionEnviadorAdapterTest` | HTTP com `MockRestServiceServer`: sucesso, `key.id`, 4xx/5xx, conexão, content-type inesperado |
| `ListarAgendamentosUseCaseImplTest` | Período em ordem com paciente, filtro por status, validação de período |
| `ModeloMensagensTest` | Pedido sem menção a áudio; orientação para resposta não reconhecida |
| `ChaveApiFiltroTest` | 401 sem chave ou com chave errada, 200 com a chave, webhook e documentação liberados, não sobe sem `API_KEY` |
| `MensagemControllerTest`, `AgendamentoControllerTest` | `POST /api/agendamentos` (201, `Location`, paciente reaproveitado, 400) e status HTTP 200/400/404/502 |
| `EvolutionWebhookControllerTest` | Token, `fromMe`, grupos, `@lid`, eventos ignorados |
| `AtendimentoTesteControllerTest` | Fluxo completo: criar → disparar → simular resposta |
| `PersistenciaPostgresAdapterTest` | Adaptadores Postgres num banco real (Testcontainers + Flyway): ida e volta das entidades, busca por CPF e telefone, CPF único, agendamentos do dia, FK, `jaEnviado`, `ultimoEnvio` sem o 9º dígito |
| `TransacaoIntegracaoTest` | No Postgres real: rollback do paciente quando o agendamento falha; duas respostas simultâneas aplicadas uma única vez |
| `DispararLembretesDoDiaUseCaseImplTest` | Rotina diária: datas de cada lembrete, não reenvia, reenvia falhas, segue após erro, ignora cancelados |
| `DisparoDiarioAgendadorTest` | Agendador usa a data do fuso configurado e não propaga falhas |
| `LivaApplicationTests` | Contexto Spring sobe com Flyway, validação do schema e agendador registrado |
| `TriagemTest`, `EncaminhamentoTest`, `ConsultaTest`, `DisponibilidadeTest` | Máquinas de estado da triagem, do encaminhamento e da consulta; transições inválidas; geração de horários |
| `SegurancaIntegracaoTest` | Spring Security real com MockMvc: 401 sem token, token adulterado, API key só nas rotas de sistema, permissões por papel, bloqueio após 5 senhas erradas, portal sem código antes da triagem, fluxo completo pela API (cadastro → triagem → encaminhamento → código pelo WhatsApp → ativação → código não reutilizável → horários → agendamento idempotente → agenda do especialista → cancelamento com versão) |
| `FilaDeEsperaIntegracaoTest` | No Postgres real: uma fila por ramo e ordem por prioridade, oferta e aceite que antecipa a consulta com oferta em cascata, recusa e expiração sem perder a posição, aceites e agendamento simultâneos no mesmo horário, ciclos concorrentes (uma oferta por vaga), horário ocupado durante a oferta, "ACEITO" repetido pelo WhatsApp, política sem resposta (manter, liberar só com entrega confirmada, confirmação na recepção), retomada após reinício, permissões por papel |
| `FluxoAssistencialIntegracaoTest` | No Postgres real, com relógio controlado: cadastro e CPF duplicado (inclusive simultâneo), conflito de versão, portal só após a primeira triagem finalizada, fila única por paciente, fluxo completo até a alta com retorno, caso grave ao plantão, segunda triagem como ramo independente, acessos negados, mesmo horário disputado por 5 pacientes ao mesmo tempo (só um reserva), idempotência, rollback da remarcação, resposta pelo WhatsApp liberando o encaminhamento, falta |

---

## Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| Mensagem "enviada" mas não chega ao celular, instância sempre `open` sem QR code | Instância criada com integração `EVOLUTION` | Apague e recrie com `WHATSAPP-BAILEYS` |
| Mensagem não aparece como conversa nova | Destino é o próprio número conectado | Veja o chat "Você" ou teste com outro número |
| `502` com `Connection Closed` | Conexão da Evolution com o WhatsApp caiu | `docker compose restart evolution-api` (não precisa ler o QR de novo) |
| `502` com `HTTP 404 ... instance does not exist` | `EVOLUTION_INSTANCIA` diferente da instância criada | Ajuste o `.env` e reinicie a Liva |
| `502` com `Connection refused` | Evolution parada | `docker compose up -d` |
| `400 Telefone do cuidador inválido: "string"` | Campo opcional enviado com o valor de exemplo do Swagger | Remova o campo do JSON |
| Resposta no WhatsApp não muda o status | Webhook não configurado ou agendamento não está `AGUARDANDO_RESPOSTA` | Veja o log `Webhook Evolution [...]`; crie um novo atendimento de teste |
| `401 Chave de API ausente ou inválida` | Faltou o header `X-API-Key` | No Swagger use **Authorize**; no curl, `-H "X-API-Key: $API_KEY"` |
| Liva não sobe: `API_KEY não configurada` | `API_KEY` vazia no `.env` | `echo "API_KEY=$(openssl rand -hex 32)" >> .env` |
| Liva não sobe: `Connection to localhost:5433 refused` | `liva-postgres` parado | `docker compose up -d liva-postgres` |
| Liva não sobe: `Schema-validation: missing column ...` | Entidade alterada sem migração correspondente | Crie uma migração `V<n>__...sql` com a alteração |
| `./mvnw test` falha com `Could not find a valid Docker environment` | Docker parado (Testcontainers) | `colima start` |
| Log `Remetente não é um contato individual` | Evento de grupo, status ou lista de transmissão | Esperado; o `jid` no log mostra a origem |
| Webhook não chega na Liva | Container não alcança o host | Confirme `extra_hosts` no compose e teste: `docker exec evolution-api wget -qO- http://host.docker.internal:8081/v3/api-docs` |

Cada chamada do webhook gera uma linha de log com o evento, o JID e o resultado:

```
Webhook Evolution [evento=messages.upsert, jid=555193953661@s.whatsapp.net, jidAlt=-, fromMe=true]: ResultadoResposta[aplicada=true, ..., status=CONFIRMADO, retornoEnviado=true, motivo=null]
```

---

## Roteiro da apresentação

O pitch de 5 minutos, com falas, tempos, plano B e perguntas prováveis, está em [docs/APRESENTACAO.md](docs/APRESENTACAO.md). O cenário da demonstração é montado com `scripts/preparar-apresentacao.sh +55DDDNUMERO`.

O diagrama relacional do banco, com todas as tabelas, colunas e chaves, está em [docs/DIAGRAMA_DO_BANCO.md](docs/DIAGRAMA_DO_BANCO.md). Para gerar de novo depois de uma migração, com o banco no ar: `python3 scripts/diagrama-do-banco.py`.

Preparação (antes de subir ao palco):

1. `docker compose up -d` e confira `curl localhost:8080/instance/connectionState/liva -H "apikey: $EVOLUTION_API_KEY"` → `open`.
2. `./mvnw spring-boot:run` e confira `curl localhost:8081/actuator/health` → `{"status":"UP"}`.
3. Abra o Swagger (http://localhost:8081/swagger-ui.html), clique em **Authorize** e cole a `API_KEY`.
4. Deixe o WhatsApp do celular de demonstração aberto (ou o chat "Você", se for o mesmo número da Evolution).

Demonstração (~5 minutos):

| # | Ação | O que mostrar |
|---|---|---|
| 1 | `POST /api/agendamentos` com o seu número, CPF e data **daqui a 2 dias** | 201; no celular chegam a confirmação do agendamento e o pedido "1, 2 ou 3" |
| 2 | Responda **"talvez"** no WhatsApp | A Liva responde "não entendi" e lista as opções |
| 3 | Responda **1** | Mensagem "Sua presença está confirmada…" |
| 4 | `GET /api/agendamentos?inicio=<data>` | Visão da equipe: paciente `CONFIRMADO`, `porStatus` |
| 5 | Repita o passo 1 com o mesmo CPF e outra data, e responda **3** | `pacienteNovo: false`; status `REAGENDADO` aparece para a equipe com `status=REAGENDADO` |
| 6 | `POST /api/disparos/executar` | A rotina diária: lembretes pendentes saem uma única vez (rode duas vezes: a segunda envia 0) |
| 7 | Chamada sem `X-API-Key` | `401` no formato padrão de erro |

Prevenção de faltas pela tela da equipe (~3 minutos, entrando como administração):

| # | Ação | O que mostrar |
|---|---|---|
| 1 | Abra **Prevenção de faltas** | O destaque de consultas que deixaram de ficar vazias, a queda da taxa de faltas e as vagas reaproveitadas |
| 2 | Em **Ligar primeiro**, aponte a primeira pessoa | A nota de risco e os motivos: faltas anteriores, pedido sem resposta, WhatsApp não entregue |
| 3 | Toque em **Confirmou** | A pessoa passa para "Confirmado" e cai para o fim da lista |
| 4 | Na seguinte, toque em **Não atendeu** | A nota sobe 10 pontos e aparece "Não atendeu a ligação" |
| 5 | Mostre **Por que as pessoas faltam** | Transporte e trabalho na frente, cada um com a ação sugerida para a unidade |
| 6 | Registre uma falta na agenda do especialista com o seu número | No celular chega "Sentimos sua falta", com o link para remarcar; responda **2** e o motivo Transporte entra no painel |
| 7 | Mostre a confirmação de um agendamento no celular | Os links "Como chegar" e "Salvar na agenda do celular" |

Se a internet ou o WhatsApp falharem no palco, use `WHATSAPP_PROVEDOR=console` e `POST /api/teste/atendimentos/respostas` para simular as respostas: o fluxo e os status são os mesmos, e as mensagens aparecem no log com `[WHATSAPP FAKE]`.

---

## Limitações conhecidas e próximos passos

- **Envio de lembretes sem garantia de entrega**: no disparo, a mensagem é enviada e depois o registro é gravado. Se a Liva cair entre os dois passos, o envio não fica registrado. Para garantir isso seria necessário o padrão Outbox.
- **Agendamentos de amanhã criados depois da rotina das 08:00** não recebem o lembrete da véspera (só a confirmação e o pedido).
- **Respostas por áudio** não são interpretadas (a mensagem pede número ou texto).
- **Com a automação da fila ligada**, as vagas canceladas dos dados de demonstração são oferecidas pelo WhatsApp aos telefones cadastrados. Use `WHATSAPP_PROVEDOR=console` ou `FILA_AUTOMACAO_HABILITADA=false` se não quiser mensagens reais.
- **Agendador em uma única instância**: com mais de uma Liva rodando, todas executariam a rotina ao mesmo tempo. O `jaEnviado` reduz, mas não elimina, envios duplicados; para escalar seria preciso um lock distribuído (ex.: ShedLock).
- **`REAGENDADO`** apenas registra a intenção; o reagendamento em si é feito pela equipe.
- **Uma única chave de API** para todos os clientes, sem usuários nem perfis. Para várias equipes/unidades seria preciso autenticação por usuário (ex.: Spring Security com JWT).
- **Endpoints de teste** devem ser desligados em produção (`ATENDIMENTO_TESTE_HABILITADO=false`), assim como `WEBHOOK_ACEITAR_MENSAGENS_PROPRIAS=false`.
# liva
