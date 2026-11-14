CREATE TABLE entradas_fila (
    id                   UUID         PRIMARY KEY,
    encaminhamento_id    UUID         NOT NULL,
    triagem_id           UUID         NOT NULL,
    paciente_id          UUID         NOT NULL,
    ciclo                INTEGER      NOT NULL CHECK (ciclo >= 0),
    especialidade_codigo VARCHAR(40)  NOT NULL REFERENCES especialidades (codigo),
    profissional_id      UUID         REFERENCES profissionais (id),
    prioridade           VARCHAR(20)  NOT NULL CHECK (prioridade IN ('EMERGENCIA', 'MUITO_URGENTE', 'URGENTE', 'POUCO_URGENTE', 'NAO_URGENTE')),
    natureza             VARCHAR(20)  NOT NULL CHECK (natureza IN ('INICIAL', 'RETORNO')),
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('AGUARDANDO', 'PAUSADA', 'ATENDIDA', 'ENCERRADA')),
    entrou_em            TIMESTAMPTZ  NOT NULL,
    motivo               VARCHAR(500),
    atualizada_em        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_entrada_do_ramo FOREIGN KEY (encaminhamento_id, triagem_id, paciente_id)
        REFERENCES encaminhamentos (id, triagem_id, paciente_id),
    CONSTRAINT uk_entrada_por_necessidade UNIQUE (encaminhamento_id, ciclo)
);

CREATE INDEX idx_entradas_fila_ordem ON entradas_fila (especialidade_codigo, status, entrou_em, id);
CREATE INDEX idx_entradas_fila_paciente ON entradas_fila (paciente_id, status);

CREATE TABLE vagas_liberadas (
    id                    UUID         PRIMARY KEY,
    agendamento_origem_id UUID         NOT NULL REFERENCES agendamentos (id),
    profissional_id       UUID         NOT NULL REFERENCES profissionais (id),
    especialidade_codigo  VARCHAR(40)  NOT NULL REFERENCES especialidades (codigo),
    inicio                TIMESTAMP    NOT NULL,
    duracao_minutos       INTEGER      NOT NULL CHECK (duracao_minutos BETWEEN 5 AND 240),
    unidade               VARCHAR(150) NOT NULL,
    endereco              VARCHAR(255) NOT NULL,
    origem                VARCHAR(30)  NOT NULL CHECK (origem IN ('CANCELAMENTO', 'SEM_CONFIRMACAO', 'ANTECIPACAO')),
    status                VARCHAR(20)  NOT NULL CHECK (status IN ('ABERTA', 'PREENCHIDA', 'ENCERRADA', 'INDISPONIVEL')),
    motivo                VARCHAR(300),
    liberada_em           TIMESTAMPTZ  NOT NULL,
    proxima_tentativa_em  TIMESTAMPTZ  NOT NULL,
    encerrada_em          TIMESTAMPTZ,
    CONSTRAINT uk_vaga_por_origem UNIQUE (agendamento_origem_id)
);

CREATE UNIQUE INDEX uk_vaga_aberta_por_horario ON vagas_liberadas (profissional_id, inicio) WHERE status = 'ABERTA';
CREATE INDEX idx_vagas_para_ofertar ON vagas_liberadas (proxima_tentativa_em) WHERE status = 'ABERTA';
CREATE INDEX idx_vagas_liberada_em ON vagas_liberadas (liberada_em);

CREATE TABLE ofertas_de_vaga (
    id                   UUID         PRIMARY KEY,
    vaga_id              UUID         NOT NULL REFERENCES vagas_liberadas (id),
    entrada_id           UUID         NOT NULL REFERENCES entradas_fila (id),
    paciente_id          UUID         NOT NULL REFERENCES pacientes (id),
    consulta_anterior_id UUID         REFERENCES agendamentos (id),
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('PENDENTE', 'ENVIADA', 'ACEITA', 'RECUSADA', 'EXPIRADA', 'CANCELADA')),
    criada_em            TIMESTAMPTZ  NOT NULL,
    expira_em            TIMESTAMPTZ  NOT NULL,
    envio_tentado_em     TIMESTAMPTZ,
    enviada_em           TIMESTAMPTZ,
    id_externo           VARCHAR(100),
    respondida_em        TIMESTAMPTZ,
    canal_resposta       VARCHAR(20)  CHECK (canal_resposta IN ('PORTAL', 'WHATSAPP')),
    motivo               VARCHAR(300),
    consulta_nova_id     UUID         REFERENCES agendamentos (id),
    CONSTRAINT uk_oferta_por_vaga_e_entrada UNIQUE (vaga_id, entrada_id),
    CONSTRAINT ck_oferta_expira_depois CHECK (expira_em > criada_em),
    CONSTRAINT ck_oferta_aceita_tem_consulta CHECK (status <> 'ACEITA' OR consulta_nova_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_oferta_ativa_por_vaga ON ofertas_de_vaga (vaga_id) WHERE status IN ('PENDENTE', 'ENVIADA');
CREATE UNIQUE INDEX uk_oferta_ativa_por_entrada ON ofertas_de_vaga (entrada_id) WHERE status IN ('PENDENTE', 'ENVIADA');
CREATE INDEX idx_ofertas_ativas_expiracao ON ofertas_de_vaga (expira_em) WHERE status IN ('PENDENTE', 'ENVIADA');
CREATE INDEX idx_ofertas_paciente ON ofertas_de_vaga (paciente_id, criada_em DESC);
CREATE INDEX idx_ofertas_consulta_anterior ON ofertas_de_vaga (consulta_anterior_id) WHERE status = 'ACEITA';

CREATE TABLE movimentacoes_fila (
    id             BIGSERIAL    PRIMARY KEY,
    entrada_id     UUID         REFERENCES entradas_fila (id),
    oferta_id      UUID         REFERENCES ofertas_de_vaga (id),
    vaga_id        UUID         REFERENCES vagas_liberadas (id),
    agendamento_id UUID         REFERENCES agendamentos (id),
    paciente_id    UUID         REFERENCES pacientes (id),
    evento         VARCHAR(40)  NOT NULL,
    detalhe        VARCHAR(500),
    ator_id        UUID,
    papel          VARCHAR(20),
    registrada_em  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_movimentacoes_entrada ON movimentacoes_fila (entrada_id, registrada_em);
CREATE INDEX idx_movimentacoes_registrada ON movimentacoes_fila (registrada_em DESC);

CREATE TABLE confirmacoes_de_presenca (
    agendamento_id UUID         PRIMARY KEY REFERENCES agendamentos (id),
    decisao        VARCHAR(30)  NOT NULL CHECK (decisao IN ('MANTIDA_SEM_RESPOSTA', 'SEM_CONTATO', 'LIBERADA', 'CONFIRMADA_NA_RECEPCAO')),
    decidida_em    TIMESTAMPTZ  NOT NULL,
    ator_id        UUID,
    papel          VARCHAR(20)
);

CREATE TABLE tarefas_executadas (
    chave        VARCHAR(120) PRIMARY KEY,
    executada_em TIMESTAMPTZ  NOT NULL
);

ALTER TABLE registros_mensagem ADD COLUMN entregue_em TIMESTAMPTZ;

CREATE INDEX idx_agendamentos_aguardando_resposta ON agendamentos (data_hora) WHERE status = 'AGUARDANDO_RESPOSTA';
CREATE INDEX idx_agendamentos_liberados ON agendamentos (data_hora) WHERE status IN ('CANCELADO', 'REAGENDADO') AND profissional_id IS NOT NULL;
