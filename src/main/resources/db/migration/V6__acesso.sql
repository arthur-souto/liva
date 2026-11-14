CREATE TABLE usuarios (
    id               UUID         PRIMARY KEY,
    login            VARCHAR(150) NOT NULL,
    nome             VARCHAR(150) NOT NULL,
    senha_hash       VARCHAR(100) NOT NULL,
    papel            VARCHAR(20)  NOT NULL CHECK (papel IN ('ADMINISTRADOR', 'RECEPCAO', 'CLINICO_GERAL', 'ESPECIALISTA', 'PACIENTE')),
    profissional_id  UUID         REFERENCES profissionais (id),
    paciente_id      UUID         REFERENCES pacientes (id),
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    tentativas_falhas INTEGER     NOT NULL DEFAULT 0,
    bloqueado_ate    TIMESTAMPTZ,
    criado_em        TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_usuario_vinculo CHECK (
        (papel = 'PACIENTE' AND paciente_id IS NOT NULL AND profissional_id IS NULL)
        OR (papel IN ('CLINICO_GERAL', 'ESPECIALISTA') AND profissional_id IS NOT NULL AND paciente_id IS NULL)
        OR (papel IN ('ADMINISTRADOR', 'RECEPCAO') AND profissional_id IS NULL AND paciente_id IS NULL))
);

CREATE UNIQUE INDEX uk_usuarios_login ON usuarios (lower(login));
CREATE UNIQUE INDEX uk_usuarios_paciente ON usuarios (paciente_id) WHERE paciente_id IS NOT NULL;
CREATE UNIQUE INDEX uk_usuarios_profissional ON usuarios (profissional_id) WHERE profissional_id IS NOT NULL;

CREATE TABLE codigos_portal (
    paciente_id UUID        PRIMARY KEY REFERENCES pacientes (id),
    codigo_hash VARCHAR(100) NOT NULL,
    expira_em   TIMESTAMPTZ NOT NULL,
    enviado_em  TIMESTAMPTZ NOT NULL,
    tentativas  INTEGER     NOT NULL DEFAULT 0
);
