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
    └── config                          UseCaseConfig (beans), DemoDataConfig, OpenApiConfig

src/main/resources/db/migration       Migrações do Flyway (V1__schema_inicial.sql)
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

## Pré-requisitos

- Java 25
- Docker com Docker Compose (no macOS, por exemplo, via Colima: `colima start`)
- Um número de WhatsApp para conectar na Evolution (pode ser o seu)

---

## Configuração

Toda a configuração vem do arquivo `.env` na raiz do projeto. Ele é lido **tanto pelo Docker Compose quanto pela Liva** (via `spring.config.import`), então não é preciso exportar variáveis. O `.env` está no `.gitignore`; use o `.env.example` como modelo.

| Variável | Usada por | Descrição |
|---|---|---|
| `API_KEY` | Liva | Chave exigida no header `X-API-Key` em todas as rotas `/api/**` (exceto o webhook). **Obrigatória**: sem ela a Liva não sobe. Gere com `openssl rand -hex 32` |
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
  -d "{\"webhook\": {\"enabled\": true, \"url\": \"$WEBHOOK_URL?token=$WEBHOOK_TOKEN\", \"byEvents\": false, \"base64\": false, \"events\": [\"MESSAGES_UPSERT\"]}}"
```

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

Ao subir, o Flyway aplica as migrações pendentes. Na primeira vez, a Liva também cria um agendamento de demonstração (`3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02`, paciente Maria da Silva) com o telefone de `confirmasus.demo.telefone` no `application.yaml`; nas próximas, ele é mantido como está. Para testes, prefira criar atendimentos novos pelo endpoint de teste.

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

- Toda rota `/api/**` exige o header `X-API-Key` com o valor de `API_KEY`; sem ele, `401` com `codigo: NAO_AUTORIZADO`. A comparação é em tempo constante.
- O webhook (`/api/webhooks/**`) não usa a chave: é chamado pela Evolution e é protegido pelo `?token=WEBHOOK_TOKEN`.
- O Swagger (`/swagger-ui.html`, `/v3/api-docs`) fica aberto para leitura, mas as chamadas feitas por ele também precisam da chave (botão **Authorize**).
- **CORS**: só as origens de `CORS_ORIGENS_PERMITIDAS` podem chamar a API pelo navegador (`CorsConfig`). O CORS roda antes da verificação da chave, então o preflight (`OPTIONS`) passa sem `X-API-Key` e o front consegue ler as respostas de erro, inclusive o 401. Headers aceitos: `Content-Type`, `Accept`, `X-API-Key`; o `Location` é exposto ao front.
- A verificação é um filtro de servlet (`ChaveApiFiltro`, em `adapter/in/web/seguranca`), sem Spring Security: há um único cliente (o sistema da unidade/equipe), então uma chave compartilhada basta para o MVP.

---

## Regras de negócio

### Lembretes (`TipoLembrete`)

| Tipo | Quando (rotina do dia) | Conteúdo |
|---|---|---|
| `CONFIRMACAO_AGENDAMENTO` | Ao criar o agendamento | Data, hora, endereço, documentos |
| `PEDIDO_CONFIRMACAO` | 3 dias antes | Pergunta "Você consegue ir? 1, 2 ou 3". Muda o status para `AGUARDANDO_RESPOSTA` |
| `LEMBRETE_VESPERA` | 1 dia antes | Data, hora, endereço e preparo |

A antecedência de cada lembrete vem de `TipoLembrete.getDiasAntes()`.

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

Se a internet ou o WhatsApp falharem no palco, use `WHATSAPP_PROVEDOR=console` e `POST /api/teste/atendimentos/respostas` para simular as respostas: o fluxo e os status são os mesmos, e as mensagens aparecem no log com `[WHATSAPP FAKE]`.

---

## Limitações conhecidas e próximos passos

- **Envio de lembretes sem garantia de entrega**: no disparo, a mensagem é enviada e depois o registro é gravado. Se a Liva cair entre os dois passos, o envio não fica registrado. Para garantir isso seria necessário o padrão Outbox.
- **Agendamentos de amanhã criados depois da rotina das 08:00** não recebem o lembrete da véspera (só a confirmação e o pedido).
- **Respostas por áudio** não são interpretadas (a mensagem pede número ou texto).
- **Agendador em uma única instância**: com mais de uma Liva rodando, todas executariam a rotina ao mesmo tempo. O `jaEnviado` reduz, mas não elimina, envios duplicados; para escalar seria preciso um lock distribuído (ex.: ShedLock).
- **`REAGENDADO`** apenas registra a intenção; o reagendamento em si é feito pela equipe.
- **Uma única chave de API** para todos os clientes, sem usuários nem perfis. Para várias equipes/unidades seria preciso autenticação por usuário (ex.: Spring Security com JWT).
- **Endpoints de teste** devem ser desligados em produção (`ATENDIMENTO_TESTE_HABILITADO=false`), assim como `WEBHOOK_ACEITAR_MENSAGENS_PROPRIAS=false`.
# liva
