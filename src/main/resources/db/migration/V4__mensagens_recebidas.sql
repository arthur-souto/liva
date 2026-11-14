CREATE TABLE mensagens_recebidas (
    id             BIGSERIAL PRIMARY KEY,
    agendamento_id UUID REFERENCES agendamentos (id),
    remetente      VARCHAR(20)   NOT NULL,
    texto          VARCHAR(4000) NOT NULL,
    id_externo     VARCHAR(100),
    resposta       VARCHAR(20),
    recebida_em    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_recebidas_agendamento ON mensagens_recebidas (agendamento_id, recebida_em);
