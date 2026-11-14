# Diagrama relacional do banco

Gerado a partir do esquema do PostgreSQL por `scripts/diagrama-do-banco.py`: 27 tabelas, 264 colunas e 48 chaves estrangeiras. A tabela de controle do Flyway não aparece.

Como ler:

- `PK` é a chave primária, `FK` é a chave estrangeira e `UK` é um valor que não se repete.
- `||--o{` é um para muitos; `||--o|` é um para no máximo um.
- `|o` do lado de cima quer dizer que a ligação é opcional: a coluna pode ficar vazia.
- "única com condição" vale só para parte das linhas, por exemplo apenas as ativas.

## Visão geral

Todas as tabelas e como se ligam. As colunas estão nas seções seguintes.

```mermaid
erDiagram
    pacientes ||--o{ agendamentos : ""
    agendamentos |o--o{ registros_mensagem : ""
    agendamentos |o--o{ mensagens_recebidas : ""
    especialidades |o--o{ profissionais : ""
    profissionais ||--o{ disponibilidades : ""
    pacientes ||--o{ triagens : ""
    profissionais |o--o{ triagens : ""
    triagens ||--o{ retificacoes_triagem : ""
    profissionais ||--o{ retificacoes_triagem : ""
    especialidades ||--o{ encaminhamentos : ""
    profissionais |o--o{ encaminhamentos : ""
    triagens ||--o{ encaminhamentos : ""
    profissionais |o--o{ agendamentos : ""
    encaminhamentos |o--o{ agendamentos : ""
    agendamentos ||--o{ evolucoes_clinicas : ""
    profissionais ||--o{ evolucoes_clinicas : ""
    profissionais |o--o| usuarios : ""
    pacientes |o--o| usuarios : ""
    pacientes ||--o| codigos_portal : ""
    pacientes ||--o{ links_de_acesso : ""
    pacientes ||--o{ remarcacoes : ""
    agendamentos ||--o{ remarcacoes : ""
    pacientes |o--o{ conversas_whatsapp : ""
    cuidadores ||--o{ vinculos_de_cuidado : ""
    pacientes ||--o{ vinculos_de_cuidado : ""
    especialidades ||--o{ entradas_fila : ""
    profissionais |o--o{ entradas_fila : ""
    encaminhamentos ||--o{ entradas_fila : ""
    agendamentos ||--o| vagas_liberadas : ""
    profissionais ||--o{ vagas_liberadas : ""
    especialidades ||--o{ vagas_liberadas : ""
    vagas_liberadas ||--o{ ofertas_de_vaga : ""
    entradas_fila ||--o{ ofertas_de_vaga : ""
    pacientes ||--o{ ofertas_de_vaga : ""
    agendamentos |o--o{ ofertas_de_vaga : ""
    entradas_fila |o--o{ movimentacoes_fila : ""
    ofertas_de_vaga |o--o{ movimentacoes_fila : ""
    vagas_liberadas |o--o{ movimentacoes_fila : ""
    agendamentos |o--o{ movimentacoes_fila : ""
    pacientes |o--o{ movimentacoes_fila : ""
    pacientes ||--o{ motivos_de_ausencia : ""
    agendamentos ||--o| confirmacoes_de_presenca : ""
    agendamentos ||--o| motivos_de_ausencia : ""
    agendamentos ||--o{ ligacoes_de_prevencao : ""
    auditoria {
        bigint id PK
    }
    tarefas_executadas {
        varchar chave PK
    }
```

## Cadastro, acesso e auditoria

Quem é atendido, quem trabalha na unidade, como cada um entra no sistema e o registro de tudo o que a equipe faz.

```mermaid
erDiagram
    pacientes {
        uuid id PK
        varchar nome_completo
        varchar nome_social "opcional"
        varchar cpf UK "opcional"
        varchar cartao_sus UK "opcional"
        date data_nascimento "opcional"
        varchar sexo "opcional"
        varchar email "opcional"
        varchar telefone
        varchar cep "opcional"
        varchar logradouro "opcional"
        varchar numero "opcional"
        varchar complemento "opcional"
        varchar bairro "opcional"
        varchar cidade "opcional"
        varchar uf "opcional"
        varchar nome_cuidador "opcional"
        varchar telefone_cuidador "opcional"
        varchar contato_emergencia_nome "opcional"
        varchar contato_emergencia_telefone "opcional"
        varchar observacoes "opcional"
        boolean consentimento
        timestamptz criado_em
        uuid criado_por "opcional"
        timestamptz atualizado_em "opcional"
        uuid atualizado_por "opcional"
        bigint version
    }
    usuarios {
        uuid id PK
        varchar login UK
        varchar nome
        varchar senha_hash
        varchar papel
        uuid profissional_id FK, UK "opcional"
        uuid paciente_id FK, UK "opcional"
        boolean ativo
        int tentativas_falhas
        timestamptz bloqueado_ate "opcional"
        timestamptz criado_em
        bigint version
    }
    profissionais {
        uuid id PK
        varchar nome
        varchar registro_conselho UK
        varchar tipo
        varchar especialidade_codigo FK "opcional"
        varchar unidade
        varchar endereco
        boolean ativo
    }
    especialidades {
        varchar codigo PK
        varchar nome
        boolean plantao
    }
    disponibilidades {
        uuid id PK
        uuid profissional_id FK
        smallint dia_semana
        time hora_inicio
        time hora_fim
        int duracao_minutos
    }
    cuidadores {
        uuid id PK
        varchar nome
        varchar cpf UK
        varchar telefone
        varchar senha_hash "opcional"
        int tentativas_falhas
        timestamptz bloqueado_ate "opcional"
        varchar codigo_hash "opcional"
        timestamptz codigo_expira_em "opcional"
        timestamptz codigo_enviado_em "opcional"
        int codigo_tentativas
        timestamptz criado_em
        timestamptz atualizado_em
    }
    vinculos_de_cuidado {
        uuid cuidador_id PK, FK
        uuid paciente_id PK, FK
        varchar parentesco
        boolean ativo
        uuid autorizado_por
        timestamptz autorizado_em
        timestamptz revogado_em "opcional"
    }
    codigos_portal {
        uuid paciente_id PK, FK
        varchar codigo_hash
        timestamptz expira_em
        timestamptz enviado_em
        int tentativas
    }
    links_de_acesso {
        varchar token_hash PK
        uuid paciente_id FK
        varchar destino
        timestamptz criado_em
        timestamptz expira_em
        timestamptz usado_em "opcional"
    }
    auditoria {
        bigint id PK
        uuid ator_id
        varchar papel
        varchar acao
        varchar recurso
        varchar recurso_id
        uuid paciente_id "opcional"
        timestamptz registrada_em
    }
    especialidades |o--o{ profissionais : "especialidade_codigo"
    profissionais ||--o{ disponibilidades : "profissional_id"
    profissionais |o--o| usuarios : "profissional_id"
    pacientes |o--o| usuarios : "paciente_id"
    pacientes ||--o| codigos_portal : "paciente_id"
    pacientes ||--o{ links_de_acesso : "paciente_id"
    cuidadores ||--o{ vinculos_de_cuidado : "cuidador_id"
    pacientes ||--o{ vinculos_de_cuidado : "paciente_id"
```

## Fluxo assistencial

Da chegada na unidade à consulta com o especialista: triagem, encaminhamento, agendamento, evolução e remarcação.

Tabelas de outras áreas aparecem só com a chave primária: `especialidades`, `pacientes`, `profissionais`.

```mermaid
erDiagram
    triagens {
        uuid id PK
        uuid paciente_id FK, UK "única com condição"
        varchar status
        uuid registrada_por
        timestamptz chegada_em
        uuid profissional_id FK "opcional"
        timestamptz iniciada_em "opcional"
        timestamptz finalizada_em "opcional"
        timestamptz encerrada_em "opcional"
        timestamptz cancelada_em "opcional"
        varchar motivo_cancelamento "opcional"
        varchar queixa_principal "opcional"
        varchar sintomas "opcional"
        varchar historico_clinico "opcional"
        varchar antecedentes "opcional"
        varchar observacoes "opcional"
        int pressao_sistolica "opcional"
        int pressao_diastolica "opcional"
        int frequencia_cardiaca "opcional"
        int frequencia_respiratoria "opcional"
        numeric temperatura "opcional"
        int saturacao "opcional"
        varchar prioridade "opcional"
        varchar justificativa_prioridade "opcional"
        bigint version
    }
    retificacoes_triagem {
        bigint id PK
        uuid triagem_id FK
        uuid autor_id FK
        varchar texto
        varchar justificativa
        timestamptz registrada_em
    }
    encaminhamentos {
        uuid id PK
        uuid triagem_id FK
        uuid paciente_id FK
        varchar especialidade_codigo FK
        uuid profissional_destino_id FK "opcional"
        varchar modalidade
        varchar prioridade
        varchar justificativa
        varchar status
        uuid criado_por FK
        timestamptz criado_em
        uuid recebido_por FK "opcional"
        timestamptz recebido_em "opcional"
        timestamptz encerrado_em "opcional"
        varchar desfecho "opcional"
        bigint version
    }
    agendamentos {
        uuid id PK
        uuid paciente_id FK
        timestamp data_hora
        varchar tipo
        varchar unidade
        varchar endereco
        varchar preparo "opcional"
        varchar status
        uuid triagem_id FK "opcional"
        uuid encaminhamento_id FK "opcional"
        uuid profissional_id FK "opcional"
        int duracao_minutos
        varchar natureza
        varchar chave_idempotencia UK "opcional"
        bigint version
    }
    evolucoes_clinicas {
        bigint id PK
        uuid agendamento_id FK
        uuid autor_id FK
        varchar texto
        timestamptz registrada_em
    }
    remarcacoes {
        bigint id PK
        uuid paciente_id FK
        uuid consulta_anterior_id FK
        uuid consulta_nova_id FK "opcional"
        varchar canal
        timestamptz remarcada_em
    }
    especialidades {
        varchar codigo PK
    }
    pacientes {
        uuid id PK
    }
    profissionais {
        uuid id PK
    }
    pacientes ||--o{ agendamentos : "paciente_id"
    pacientes ||--o{ triagens : "paciente_id"
    profissionais |o--o{ triagens : "profissional_id"
    triagens ||--o{ retificacoes_triagem : "triagem_id"
    profissionais ||--o{ retificacoes_triagem : "autor_id"
    especialidades ||--o{ encaminhamentos : "especialidade_codigo"
    profissionais |o--o{ encaminhamentos : "profissional_destino_id"
    profissionais ||--o{ encaminhamentos : "criado_por"
    profissionais |o--o{ encaminhamentos : "recebido_por"
    triagens ||--o{ encaminhamentos : "triagem_id, paciente_id"
    profissionais |o--o{ agendamentos : "profissional_id"
    encaminhamentos |o--o{ agendamentos : "encaminhamento_id, triagem_id, paciente_id"
    agendamentos ||--o{ evolucoes_clinicas : "agendamento_id"
    profissionais ||--o{ evolucoes_clinicas : "autor_id"
    pacientes ||--o{ remarcacoes : "paciente_id"
    agendamentos ||--o{ remarcacoes : "consulta_anterior_id"
    agendamentos |o--o{ remarcacoes : "consulta_nova_id"
```

## WhatsApp e lembretes

Mensagens enviadas e recebidas, a conversa com o assistente e as rotinas automáticas.

Tabelas de outras áreas aparecem só com a chave primária: `agendamentos`, `pacientes`.

```mermaid
erDiagram
    registros_mensagem {
        bigint id PK
        uuid agendamento_id FK "opcional"
        varchar destino
        varchar chave_destino
        varchar tipo "opcional"
        varchar texto
        boolean sucesso
        varchar id_externo "opcional"
        varchar erro "opcional"
        timestamptz enviado_em
        timestamptz entregue_em "opcional"
    }
    mensagens_recebidas {
        bigint id PK
        uuid agendamento_id FK "opcional"
        varchar remetente
        varchar texto
        varchar id_externo "opcional"
        varchar resposta "opcional"
        timestamptz recebida_em
    }
    conversas_whatsapp {
        varchar chave_telefone PK
        varchar telefone
        uuid paciente_id FK "opcional"
        varchar etapa
        varchar opcoes
        timestamptz atualizada_em
    }
    tarefas_executadas {
        varchar chave PK
        timestamptz executada_em
    }
    agendamentos {
        uuid id PK
    }
    pacientes {
        uuid id PK
    }
    agendamentos |o--o{ registros_mensagem : "agendamento_id"
    agendamentos |o--o{ mensagens_recebidas : "agendamento_id"
    pacientes |o--o{ conversas_whatsapp : "paciente_id"
```

## Fila de espera e reaproveitamento de vagas

Quem espera por cada especialidade, os horários liberados, as ofertas feitas e o histórico da fila.

Tabelas de outras áreas aparecem só com a chave primária: `agendamentos`, `encaminhamentos`, `especialidades`, `pacientes`, `profissionais`.

```mermaid
erDiagram
    entradas_fila {
        uuid id PK
        uuid encaminhamento_id FK
        uuid triagem_id FK
        uuid paciente_id FK
        int ciclo
        varchar especialidade_codigo FK
        uuid profissional_id FK "opcional"
        varchar prioridade
        varchar natureza
        varchar status
        timestamptz entrou_em
        varchar motivo "opcional"
        timestamptz atualizada_em
    }
    vagas_liberadas {
        uuid id PK
        uuid agendamento_origem_id FK, UK
        uuid profissional_id FK
        varchar especialidade_codigo FK
        timestamp inicio
        int duracao_minutos
        varchar unidade
        varchar endereco
        varchar origem
        varchar status
        varchar motivo "opcional"
        timestamptz liberada_em
        timestamptz proxima_tentativa_em
        timestamptz encerrada_em "opcional"
    }
    ofertas_de_vaga {
        uuid id PK
        uuid vaga_id FK, UK "única com condição"
        uuid entrada_id FK, UK "única com condição"
        uuid paciente_id FK
        uuid consulta_anterior_id FK "opcional"
        varchar status
        timestamptz criada_em
        timestamptz expira_em
        timestamptz envio_tentado_em "opcional"
        timestamptz enviada_em "opcional"
        varchar id_externo "opcional"
        timestamptz respondida_em "opcional"
        varchar canal_resposta "opcional"
        varchar motivo "opcional"
        uuid consulta_nova_id FK "opcional"
    }
    movimentacoes_fila {
        bigint id PK
        uuid entrada_id FK "opcional"
        uuid oferta_id FK "opcional"
        uuid vaga_id FK "opcional"
        uuid agendamento_id FK "opcional"
        uuid paciente_id FK "opcional"
        varchar evento
        varchar detalhe "opcional"
        uuid ator_id "opcional"
        varchar papel "opcional"
        timestamptz registrada_em
    }
    confirmacoes_de_presenca {
        uuid agendamento_id PK, FK
        varchar decisao
        timestamptz decidida_em
        uuid ator_id "opcional"
        varchar papel "opcional"
    }
    agendamentos {
        uuid id PK
    }
    encaminhamentos {
        uuid id PK
    }
    especialidades {
        varchar codigo PK
    }
    pacientes {
        uuid id PK
    }
    profissionais {
        uuid id PK
    }
    especialidades ||--o{ entradas_fila : "especialidade_codigo"
    profissionais |o--o{ entradas_fila : "profissional_id"
    encaminhamentos ||--o{ entradas_fila : "encaminhamento_id, triagem_id, paciente_id"
    agendamentos ||--o| vagas_liberadas : "agendamento_origem_id"
    profissionais ||--o{ vagas_liberadas : "profissional_id"
    especialidades ||--o{ vagas_liberadas : "especialidade_codigo"
    vagas_liberadas ||--o{ ofertas_de_vaga : "vaga_id"
    entradas_fila ||--o{ ofertas_de_vaga : "entrada_id"
    pacientes ||--o{ ofertas_de_vaga : "paciente_id"
    agendamentos |o--o{ ofertas_de_vaga : "consulta_anterior_id"
    agendamentos |o--o{ ofertas_de_vaga : "consulta_nova_id"
    entradas_fila |o--o{ movimentacoes_fila : "entrada_id"
    ofertas_de_vaga |o--o{ movimentacoes_fila : "oferta_id"
    vagas_liberadas |o--o{ movimentacoes_fila : "vaga_id"
    agendamentos |o--o{ movimentacoes_fila : "agendamento_id"
    pacientes |o--o{ movimentacoes_fila : "paciente_id"
    agendamentos ||--o| confirmacoes_de_presenca : "agendamento_id"
```

## Prevenção de faltas

Por que a pessoa faltou ou cancelou e o resultado das ligações da equipe.

Tabelas de outras áreas aparecem só com a chave primária: `agendamentos`, `pacientes`.

```mermaid
erDiagram
    motivos_de_ausencia {
        uuid agendamento_id PK, FK
        uuid paciente_id FK
        varchar ocorrencia
        varchar motivo "opcional"
        varchar canal "opcional"
        timestamptz perguntado_em "opcional"
        timestamptz respondido_em "opcional"
    }
    ligacoes_de_prevencao {
        uuid id PK
        uuid agendamento_id FK
        varchar resultado
        uuid usuario_id "opcional"
        timestamptz registrada_em
    }
    agendamentos {
        uuid id PK
    }
    pacientes {
        uuid id PK
    }
    pacientes ||--o{ motivos_de_ausencia : "paciente_id"
    agendamentos ||--o| motivos_de_ausencia : "agendamento_id"
    agendamentos ||--o{ ligacoes_de_prevencao : "agendamento_id"
```
