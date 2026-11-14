CREATE TABLE motivos_de_ausencia (
    agendamento_id UUID         PRIMARY KEY REFERENCES agendamentos (id),
    paciente_id    UUID         NOT NULL REFERENCES pacientes (id),
    ocorrencia     VARCHAR(20)  NOT NULL CHECK (ocorrencia IN ('FALTA', 'CANCELAMENTO')),
    motivo         VARCHAR(30)  CHECK (motivo IN ('ESQUECI', 'TRANSPORTE', 'TRABALHO', 'SAUDE', 'NAO_PRECISA', 'OUTRO')),
    canal          VARCHAR(20)  CHECK (canal IN ('WHATSAPP', 'PORTAL', 'EQUIPE')),
    perguntado_em  TIMESTAMPTZ,
    respondido_em  TIMESTAMPTZ,
    CONSTRAINT ck_motivo_respondido CHECK ((motivo IS NULL) = (respondido_em IS NULL))
);

CREATE INDEX idx_motivos_pendentes ON motivos_de_ausencia (paciente_id, perguntado_em) WHERE motivo IS NULL;
CREATE INDEX idx_motivos_respondidos ON motivos_de_ausencia (respondido_em) WHERE motivo IS NOT NULL;
