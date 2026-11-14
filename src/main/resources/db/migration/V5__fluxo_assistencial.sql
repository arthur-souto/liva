CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE especialidades (
    codigo  VARCHAR(40)  PRIMARY KEY,
    nome    VARCHAR(100) NOT NULL,
    plantao BOOLEAN      NOT NULL DEFAULT FALSE
);

INSERT INTO especialidades (codigo, nome, plantao) VALUES
    ('ORTOPEDIA', 'Ortopedia', FALSE),
    ('CARDIOLOGIA', 'Cardiologia', FALSE),
    ('DERMATOLOGIA', 'Dermatologia', FALSE),
    ('NEUROLOGIA', 'Neurologia', FALSE),
    ('GINECOLOGIA', 'Ginecologia', FALSE),
    ('PEDIATRIA', 'Pediatria', FALSE),
    ('PRONTO_ATENDIMENTO', 'Pronto atendimento (plantão)', TRUE);

CREATE TABLE profissionais (
    id                  UUID         PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    registro_conselho   VARCHAR(30)  NOT NULL,
    tipo                VARCHAR(20)  NOT NULL CHECK (tipo IN ('CLINICO_GERAL', 'ESPECIALISTA')),
    especialidade_codigo VARCHAR(40) REFERENCES especialidades (codigo),
    unidade             VARCHAR(150) NOT NULL,
    endereco            VARCHAR(255) NOT NULL,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_profissional_especialidade CHECK (tipo = 'CLINICO_GERAL' OR especialidade_codigo IS NOT NULL)
);

CREATE UNIQUE INDEX uk_profissionais_registro ON profissionais (registro_conselho);

CREATE TABLE disponibilidades (
    id              UUID     PRIMARY KEY,
    profissional_id UUID     NOT NULL REFERENCES profissionais (id),
    dia_semana      SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio     TIME     NOT NULL,
    hora_fim        TIME     NOT NULL,
    duracao_minutos INTEGER  NOT NULL CHECK (duracao_minutos BETWEEN 5 AND 240),
    CONSTRAINT ck_disponibilidade_intervalo CHECK (hora_fim > hora_inicio)
);

CREATE INDEX idx_disponibilidades_profissional ON disponibilidades (profissional_id, dia_semana);

ALTER TABLE pacientes
    ADD COLUMN criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN criado_por    UUID,
    ADD COLUMN atualizado_em TIMESTAMPTZ,
    ADD COLUMN atualizado_por UUID,
    ADD COLUMN version       BIGINT      NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_pacientes_cartao_sus ON pacientes (cartao_sus) WHERE cartao_sus IS NOT NULL;
CREATE INDEX idx_pacientes_nome ON pacientes (lower(nome_completo));

CREATE TABLE triagens (
    id                       UUID          PRIMARY KEY,
    paciente_id              UUID          NOT NULL REFERENCES pacientes (id),
    status                   VARCHAR(30)   NOT NULL CHECK (status IN ('AGUARDANDO_ATENDIMENTO', 'EM_ANDAMENTO', 'FINALIZADA', 'CANCELADA', 'ENCERRADA')),
    registrada_por           UUID          NOT NULL,
    chegada_em               TIMESTAMPTZ   NOT NULL,
    profissional_id          UUID          REFERENCES profissionais (id),
    iniciada_em              TIMESTAMPTZ,
    finalizada_em            TIMESTAMPTZ,
    encerrada_em             TIMESTAMPTZ,
    cancelada_em             TIMESTAMPTZ,
    motivo_cancelamento      VARCHAR(1000),
    queixa_principal         VARCHAR(1000),
    sintomas                 VARCHAR(2000),
    historico_clinico        VARCHAR(2000),
    antecedentes             VARCHAR(2000),
    observacoes              VARCHAR(2000),
    pressao_sistolica        INTEGER,
    pressao_diastolica       INTEGER,
    frequencia_cardiaca      INTEGER,
    frequencia_respiratoria  INTEGER,
    temperatura              NUMERIC(4, 1),
    saturacao                INTEGER,
    prioridade               VARCHAR(20)   CHECK (prioridade IN ('EMERGENCIA', 'MUITO_URGENTE', 'URGENTE', 'POUCO_URGENTE', 'NAO_URGENTE')),
    justificativa_prioridade VARCHAR(1000),
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_triagens_paciente UNIQUE (id, paciente_id),
    CONSTRAINT ck_triagem_finalizada CHECK (status NOT IN ('FINALIZADA', 'ENCERRADA')
        OR (queixa_principal IS NOT NULL AND prioridade IS NOT NULL AND profissional_id IS NOT NULL AND finalizada_em IS NOT NULL))
);

CREATE UNIQUE INDEX uk_triagem_aberta_por_paciente ON triagens (paciente_id) WHERE status IN ('AGUARDANDO_ATENDIMENTO', 'EM_ANDAMENTO');
CREATE INDEX idx_triagens_paciente ON triagens (paciente_id, chegada_em);
CREATE INDEX idx_triagens_fila ON triagens (status, chegada_em);

CREATE TABLE retificacoes_triagem (
    id            BIGSERIAL     PRIMARY KEY,
    triagem_id    UUID          NOT NULL REFERENCES triagens (id),
    autor_id      UUID          NOT NULL REFERENCES profissionais (id),
    texto         VARCHAR(2000) NOT NULL,
    justificativa VARCHAR(1000) NOT NULL,
    registrada_em TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_retificacoes_triagem ON retificacoes_triagem (triagem_id, registrada_em);

CREATE TABLE encaminhamentos (
    id                      UUID          PRIMARY KEY,
    triagem_id              UUID          NOT NULL,
    paciente_id             UUID          NOT NULL,
    especialidade_codigo    VARCHAR(40)   NOT NULL REFERENCES especialidades (codigo),
    profissional_destino_id UUID          REFERENCES profissionais (id),
    modalidade              VARCHAR(20)   NOT NULL CHECK (modalidade IN ('ELETIVO', 'IMEDIATO')),
    prioridade              VARCHAR(20)   NOT NULL CHECK (prioridade IN ('EMERGENCIA', 'MUITO_URGENTE', 'URGENTE', 'POUCO_URGENTE', 'NAO_URGENTE')),
    justificativa           VARCHAR(1000) NOT NULL,
    status                  VARCHAR(30)   NOT NULL CHECK (status IN ('PENDENTE', 'AGUARDANDO_AGENDAMENTO', 'AGENDADO', 'EM_ATENDIMENTO', 'CONCLUIDO', 'CANCELADO')),
    criado_por              UUID          NOT NULL REFERENCES profissionais (id),
    criado_em               TIMESTAMPTZ   NOT NULL,
    recebido_por            UUID          REFERENCES profissionais (id),
    recebido_em             TIMESTAMPTZ,
    encerrado_em            TIMESTAMPTZ,
    desfecho                VARCHAR(2000),
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_encaminhamento_triagem_do_paciente FOREIGN KEY (triagem_id, paciente_id) REFERENCES triagens (id, paciente_id),
    CONSTRAINT uk_encaminhamentos_ramo UNIQUE (id, triagem_id, paciente_id)
);

CREATE INDEX idx_encaminhamentos_triagem ON encaminhamentos (triagem_id);
CREATE INDEX idx_encaminhamentos_paciente ON encaminhamentos (paciente_id);
CREATE INDEX idx_encaminhamentos_especialidade ON encaminhamentos (especialidade_codigo, status);

ALTER TABLE agendamentos
    ADD COLUMN triagem_id         UUID,
    ADD COLUMN encaminhamento_id  UUID,
    ADD COLUMN profissional_id    UUID REFERENCES profissionais (id),
    ADD COLUMN duracao_minutos    INTEGER     NOT NULL DEFAULT 30 CHECK (duracao_minutos BETWEEN 5 AND 240),
    ADD COLUMN natureza           VARCHAR(20) NOT NULL DEFAULT 'AVULSA' CHECK (natureza IN ('AVULSA', 'INICIAL', 'RETORNO')),
    ADD COLUMN chave_idempotencia VARCHAR(64),
    ADD COLUMN version            BIGINT      NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_consulta_do_ramo FOREIGN KEY (encaminhamento_id, triagem_id, paciente_id)
        REFERENCES encaminhamentos (id, triagem_id, paciente_id),
    ADD CONSTRAINT ck_consulta_vinculada CHECK (
        (natureza = 'AVULSA' AND encaminhamento_id IS NULL AND triagem_id IS NULL)
        OR (natureza <> 'AVULSA' AND encaminhamento_id IS NOT NULL AND triagem_id IS NOT NULL AND profissional_id IS NOT NULL)),
    ADD CONSTRAINT ex_agenda_do_profissional EXCLUDE USING gist (
        profissional_id WITH =,
        tsrange(data_hora, data_hora + duracao_minutos * INTERVAL '1 minute') WITH &&
    ) WHERE (profissional_id IS NOT NULL AND status IN ('AGENDADO', 'AGUARDANDO_RESPOSTA', 'CONFIRMADO', 'EM_ANDAMENTO'));

CREATE UNIQUE INDEX uk_agendamentos_idempotencia ON agendamentos (chave_idempotencia) WHERE chave_idempotencia IS NOT NULL;
CREATE INDEX idx_agendamentos_encaminhamento ON agendamentos (encaminhamento_id);
CREATE INDEX idx_agendamentos_profissional ON agendamentos (profissional_id, data_hora);

CREATE TABLE evolucoes_clinicas (
    id             BIGSERIAL     PRIMARY KEY,
    agendamento_id UUID          NOT NULL REFERENCES agendamentos (id),
    autor_id       UUID          NOT NULL REFERENCES profissionais (id),
    texto          VARCHAR(4000) NOT NULL,
    registrada_em  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_evolucoes_agendamento ON evolucoes_clinicas (agendamento_id, registrada_em);

CREATE TABLE auditoria (
    id            BIGSERIAL   PRIMARY KEY,
    ator_id       UUID        NOT NULL,
    papel         VARCHAR(20) NOT NULL,
    acao          VARCHAR(60) NOT NULL,
    recurso       VARCHAR(30) NOT NULL,
    recurso_id    VARCHAR(64) NOT NULL,
    paciente_id   UUID,
    registrada_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auditoria_paciente ON auditoria (paciente_id, registrada_em);
CREATE INDEX idx_auditoria_ator ON auditoria (ator_id, registrada_em);
