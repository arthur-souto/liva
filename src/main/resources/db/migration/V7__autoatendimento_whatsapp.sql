CREATE TABLE links_de_acesso (
    token_hash  VARCHAR(64)  PRIMARY KEY,
    paciente_id UUID         NOT NULL REFERENCES pacientes (id),
    destino     VARCHAR(300) NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL,
    expira_em   TIMESTAMPTZ  NOT NULL,
    usado_em    TIMESTAMPTZ
);

CREATE INDEX idx_links_paciente ON links_de_acesso (paciente_id, criado_em);

CREATE TABLE remarcacoes (
    id                   BIGSERIAL   PRIMARY KEY,
    paciente_id          UUID        NOT NULL REFERENCES pacientes (id),
    consulta_anterior_id UUID        NOT NULL REFERENCES agendamentos (id),
    consulta_nova_id     UUID        REFERENCES agendamentos (id),
    canal                VARCHAR(20) NOT NULL CHECK (canal IN ('PORTAL', 'WHATSAPP', 'RECEPCAO')),
    remarcada_em         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_remarcacoes_paciente ON remarcacoes (paciente_id, remarcada_em);

CREATE TABLE conversas_whatsapp (
    chave_telefone VARCHAR(10)   PRIMARY KEY,
    telefone       VARCHAR(20)   NOT NULL,
    paciente_id    UUID          REFERENCES pacientes (id),
    etapa          VARCHAR(40)   NOT NULL,
    opcoes         VARCHAR(4000) NOT NULL DEFAULT '',
    atualizada_em  TIMESTAMPTZ   NOT NULL
);
