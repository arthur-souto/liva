CREATE TABLE cuidadores (
    id                UUID         PRIMARY KEY,
    nome              VARCHAR(150) NOT NULL,
    cpf               VARCHAR(11)  NOT NULL,
    telefone          VARCHAR(20)  NOT NULL,
    senha_hash        VARCHAR(100),
    tentativas_falhas INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate     TIMESTAMPTZ,
    codigo_hash       VARCHAR(100),
    codigo_expira_em  TIMESTAMPTZ,
    codigo_enviado_em TIMESTAMPTZ,
    codigo_tentativas INTEGER      NOT NULL DEFAULT 0,
    criado_em         TIMESTAMPTZ  NOT NULL,
    atualizado_em     TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uk_cuidadores_cpf ON cuidadores (cpf);
CREATE INDEX idx_cuidadores_telefone ON cuidadores (telefone);

CREATE TABLE vinculos_de_cuidado (
    cuidador_id    UUID         NOT NULL REFERENCES cuidadores (id),
    paciente_id    UUID         NOT NULL REFERENCES pacientes (id),
    parentesco     VARCHAR(30)  NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    autorizado_por UUID         NOT NULL,
    autorizado_em  TIMESTAMPTZ  NOT NULL,
    revogado_em    TIMESTAMPTZ,
    PRIMARY KEY (cuidador_id, paciente_id)
);

CREATE INDEX idx_vinculos_paciente ON vinculos_de_cuidado (paciente_id) WHERE ativo;
