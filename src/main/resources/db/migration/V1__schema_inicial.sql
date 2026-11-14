CREATE TABLE pacientes (
    id                          UUID PRIMARY KEY,
    nome_completo               VARCHAR(150) NOT NULL,
    nome_social                 VARCHAR(150),
    cpf                         VARCHAR(11),
    cartao_sus                  VARCHAR(15),
    data_nascimento             DATE,
    sexo                        VARCHAR(20),
    email                       VARCHAR(150),
    telefone                    VARCHAR(20)  NOT NULL,
    cep                         VARCHAR(9),
    logradouro                  VARCHAR(150),
    numero                      VARCHAR(10),
    complemento                 VARCHAR(100),
    bairro                      VARCHAR(100),
    cidade                      VARCHAR(100),
    uf                          VARCHAR(2),
    nome_cuidador               VARCHAR(150),
    telefone_cuidador           VARCHAR(20),
    contato_emergencia_nome     VARCHAR(150),
    contato_emergencia_telefone VARCHAR(20),
    observacoes                 VARCHAR(500),
    consentimento               BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE agendamentos (
    id          UUID PRIMARY KEY,
    paciente_id UUID         NOT NULL REFERENCES pacientes (id),
    data_hora   TIMESTAMP    NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    unidade     VARCHAR(150) NOT NULL,
    endereco    VARCHAR(255) NOT NULL,
    preparo     VARCHAR(500),
    status      VARCHAR(30)  NOT NULL
);

CREATE INDEX idx_agendamentos_data_hora ON agendamentos (data_hora);
CREATE INDEX idx_agendamentos_paciente ON agendamentos (paciente_id);

CREATE TABLE registros_mensagem (
    id             BIGSERIAL PRIMARY KEY,
    agendamento_id UUID REFERENCES agendamentos (id),
    destino        VARCHAR(20)  NOT NULL,
    chave_destino  VARCHAR(10)  NOT NULL,
    tipo           VARCHAR(30),
    texto          VARCHAR(4000) NOT NULL,
    sucesso        BOOLEAN      NOT NULL,
    id_externo     VARCHAR(100),
    erro           VARCHAR(4000),
    enviado_em     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_registros_agendamento_tipo ON registros_mensagem (agendamento_id, tipo);
CREATE INDEX idx_registros_chave_tipo_enviado ON registros_mensagem (chave_destino, tipo, enviado_em DESC);
