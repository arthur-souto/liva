CREATE TABLE ligacoes_de_prevencao (
    id              UUID         PRIMARY KEY,
    agendamento_id  UUID         NOT NULL REFERENCES agendamentos (id),
    resultado       VARCHAR(20)  NOT NULL CHECK (resultado IN ('CONFIRMOU', 'VAI_REMARCAR', 'NAO_ATENDEU')),
    usuario_id      UUID,
    registrada_em   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_ligacoes_agendamento ON ligacoes_de_prevencao (agendamento_id, registrada_em DESC);
