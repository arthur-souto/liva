INSERT INTO profissionais (id, nome, registro_conselho, tipo, especialidade_codigo, unidade, endereco, ativo) VALUES
    ('0c1d2e3f-0000-4000-8000-000000000001', 'Dr. Paulo Mendes', 'CRM-SP 100001', 'CLINICO_GERAL', NULL, 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000002', 'Dra. Ana Ribeiro', 'CRM-SP 100002', 'ESPECIALISTA', 'ORTOPEDIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000003', 'Dr. Carlos Lima', 'CRM-SP 100003', 'ESPECIALISTA', 'CARDIOLOGIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000004', 'Dra. Júlia Costa', 'CRM-SP 100004', 'ESPECIALISTA', 'PRONTO_ATENDIMENTO', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO disponibilidades (id, profissional_id, dia_semana, hora_inicio, hora_fim, duracao_minutos)
SELECT gen_random_uuid(), p.id, d.dia, t.inicio, t.fim, 30
FROM (VALUES ('0c1d2e3f-0000-4000-8000-000000000002'::uuid), ('0c1d2e3f-0000-4000-8000-000000000003'::uuid)) AS p (id)
CROSS JOIN generate_series(1, 5) AS d (dia)
CROSS JOIN (VALUES (TIME '08:00', TIME '12:00'), (TIME '13:30', TIME '17:00')) AS t (inicio, fim)
WHERE NOT EXISTS (SELECT 1 FROM disponibilidades x WHERE x.profissional_id = p.id AND x.dia_semana = d.dia AND x.hora_inicio = t.inicio);

INSERT INTO pacientes (id, nome_completo, cpf, cartao_sus, data_nascimento, sexo, email, telefone, cep, logradouro, numero, bairro, cidade, uf, consentimento) VALUES
    ('a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d', 'Maria da Silva', '12345678909', '123456789012345', DATE '1958-03-12', 'FEMININO', 'maria@exemplo.com', '+5551993953661', '01001000', 'Praça da Sé', '100', 'Sé', 'São Paulo', 'SP', TRUE),
    ('d0000000-0000-4000-8000-000000000001', 'João Pereira', '52601815906', NULL, DATE '1972-08-03', 'MASCULINO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000002', 'Ana Souza', '08301661305', NULL, DATE '1990-01-22', 'FEMININO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000003', 'Pedro Alves', '18609139034', NULL, DATE '1965-11-30', 'MASCULINO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000004', 'Luiza Martins', '99603082430', NULL, DATE '1958-06-14', 'FEMININO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000005', 'Roberto Santos', '62819482112', NULL, DATE '1980-02-09', 'MASCULINO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000006', 'Carla Nunes', '99351819019', NULL, DATE '1975-04-18', 'FEMININO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('d0000000-0000-4000-8000-000000000007', 'Francisco Lima', '93786579741', NULL, DATE '1950-09-05', 'MASCULINO', NULL, '+5551993953661', NULL, NULL, NULL, NULL, NULL, NULL, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO usuarios (id, login, nome, senha_hash, papel, profissional_id, paciente_id, criado_em) VALUES
    ('0a0b0c0d-0000-4000-8000-000000000001', 'admin@liva.demo', 'Administração', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'ADMINISTRADOR', NULL, NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000002', 'recepcao@liva.demo', 'Recepção', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'RECEPCAO', NULL, NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000003', 'clinico@liva.demo', 'Dr. Paulo Mendes', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'CLINICO_GERAL', '0c1d2e3f-0000-4000-8000-000000000001', NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000004', 'ortopedia@liva.demo', 'Dra. Ana Ribeiro', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'ESPECIALISTA', '0c1d2e3f-0000-4000-8000-000000000002', NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000005', 'cardiologia@liva.demo', 'Dr. Carlos Lima', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'ESPECIALISTA', '0c1d2e3f-0000-4000-8000-000000000003', NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000006', 'plantao@liva.demo', 'Dra. Júlia Costa', '$2a$10$h72b4Jtr8JvCxD7Y2ZN5gekLk6ADz5YQwH/tpaRTcWW6Iq6cAqYee', 'ESPECIALISTA', '0c1d2e3f-0000-4000-8000-000000000004', NULL, now()),
    ('0a0b0c0d-0000-4000-8000-000000000007', '12345678909', 'Maria da Silva', '$2a$10$jZnCkC1uybzFa2VtqexxqeT7heEZXoHkNkvVo01uHB2pRHmbQB6OC', 'PACIENTE', NULL, 'a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d', now())
ON CONFLICT DO NOTHING;

INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, preparo, status) VALUES
    ('3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02', 'a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
     (now() AT TIME ZONE 'America/Sao_Paulo')::date + 2 + TIME '09:30', 'CONSULTA', 'UBS Centro', 'Rua das Flores, 123 - Centro', 'Jejum de 8 horas', 'AGENDADO')
ON CONFLICT DO NOTHING;

INSERT INTO triagens (id, paciente_id, status, registrada_por, chegada_em, profissional_id, iniciada_em, finalizada_em, encerrada_em,
                      cancelada_em, motivo_cancelamento, queixa_principal, sintomas, historico_clinico, antecedentes, observacoes,
                      pressao_sistolica, pressao_diastolica, frequencia_cardiaca, frequencia_respiratoria, temperatura, saturacao,
                      prioridade, justificativa_prioridade)
SELECT t.id::uuid, t.paciente::uuid, t.status, '0a0b0c0d-0000-4000-8000-000000000002', t.chegada,
       CASE WHEN t.status = 'AGUARDANDO_ATENDIMENTO' OR t.status = 'CANCELADA' THEN NULL ELSE '0c1d2e3f-0000-4000-8000-000000000001'::uuid END,
       CASE WHEN t.status IN ('AGUARDANDO_ATENDIMENTO', 'CANCELADA') THEN NULL
            WHEN t.status = 'EM_ANDAMENTO' THEN now() - INTERVAL '15 minutes' ELSE t.chegada + INTERVAL '5 minutes' END,
       CASE WHEN t.status IN ('FINALIZADA', 'ENCERRADA') THEN t.chegada + INTERVAL '20 minutes' END,
       CASE WHEN t.status = 'ENCERRADA' THEN t.chegada + INTERVAL '16 days' END,
       CASE WHEN t.status = 'CANCELADA' THEN t.chegada + INTERVAL '40 minutes' END,
       CASE WHEN t.status = 'CANCELADA' THEN 'Paciente desistiu do atendimento' END,
       t.queixa, t.sintomas, t.historico, t.antecedentes, t.observacoes, t.pas, t.pad, t.fc, t.fr, t.temp, t.sat, t.prioridade, t.justificativa
FROM (VALUES
    ('d1000000-0000-4000-8000-000000000001', 'a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d', 'ENCERRADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 45 + TIME '08:10') AT TIME ZONE 'America/Sao_Paulo',
     'Dor no joelho direito há 3 meses', 'Dor ao subir escadas, rigidez matinal', 'Hipertensão controlada', 'Losartana 50 mg', NULL,
     132, 84, 76, 16, 36.4, 97, 'POUCO_URGENTE', NULL),
    ('d1000000-0000-4000-8000-000000000002', 'a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d', 'FINALIZADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 2 + TIME '09:00') AT TIME ZONE 'America/Sao_Paulo',
     'Cansaço e aperto no peito aos esforços', 'Piora ao caminhar 2 quarteirões, melhora em repouso', 'Hipertensão',
     'Pai com infarto aos 60 anos', 'ECG da UBS sem alterações agudas', 148, 92, 88, 18, 36.6, 96, 'URGENTE', NULL),
    ('d1000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'CANCELADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 1 + TIME '15:00') AT TIME ZONE 'America/Sao_Paulo',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('d1000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000001', 'AGUARDANDO_ATENDIMENTO',
     now() - INTERVAL '25 minutes', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('d1000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000002', 'AGUARDANDO_ATENDIMENTO',
     now() - INTERVAL '12 minutes', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('d1000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000003', 'EM_ANDAMENTO',
     now() - INTERVAL '40 minutes', 'Febre e tosse há 4 dias', 'Tosse produtiva, dor ao respirar fundo', NULL, 'Tabagista', NULL,
     124, 78, 102, 22, 38.4, 94, NULL, NULL),
    ('d1000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000004', 'FINALIZADA',
     now() - INTERVAL '50 minutes', 'Dor no peito em aperto há 1 hora', 'Irradia para o braço esquerdo, sudorese', 'Diabetes tipo 2',
     'Metformina', NULL, 168, 102, 112, 24, 36.9, 92, 'EMERGENCIA', 'Dor torácica típica com instabilidade hemodinâmica'),
    ('d1000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000005', 'FINALIZADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 7 + TIME '10:00') AT TIME ZONE 'America/Sao_Paulo',
     'Dor lombar há 2 semanas', 'Irradia para a perna esquerda', NULL, NULL, NULL, 128, 82, 74, 16, 36.5, 98, 'URGENTE', NULL),
    ('d1000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000006', 'FINALIZADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 5 + TIME '14:00') AT TIME ZONE 'America/Sao_Paulo',
     'Palpitações há 1 mês', 'Episódios rápidos, sem desmaio', NULL, 'Ansiedade', NULL, 122, 80, 96, 16, 36.3, 98, 'POUCO_URGENTE', NULL),
    ('d1000000-0000-4000-8000-000000000010', 'd0000000-0000-4000-8000-000000000007', 'FINALIZADA',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 20 + TIME '08:30') AT TIME ZONE 'America/Sao_Paulo',
     'Pressão alta persistente', 'Cefaleia occipital', 'Hipertensão há 10 anos', 'Enalapril e hidroclorotiazida', NULL,
     172, 104, 80, 16, 36.5, 97, 'URGENTE', NULL)
) AS t (id, paciente, status, chegada, queixa, sintomas, historico, antecedentes, observacoes, pas, pad, fc, fr, temp, sat, prioridade, justificativa)
ON CONFLICT DO NOTHING;

INSERT INTO encaminhamentos (id, triagem_id, paciente_id, especialidade_codigo, modalidade, prioridade, justificativa, status,
                             criado_por, criado_em, encerrado_em, desfecho)
SELECT e.id::uuid, t.id, t.paciente_id, e.especialidade, e.modalidade, e.prioridade, e.justificativa, e.status,
       '0c1d2e3f-0000-4000-8000-000000000001', t.chegada_em + INTERVAL '15 minutes',
       CASE WHEN e.status = 'CONCLUIDO' THEN t.chegada_em + INTERVAL '15 days' END, e.desfecho
FROM (VALUES
    ('d2000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000001', 'ORTOPEDIA', 'ELETIVO', 'POUCO_URGENTE',
     'Gonartrose? Avaliar e definir conduta', 'CONCLUIDO', 'Gonartrose leve. Fisioterapia e retorno à UBS.'),
    ('d2000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000002', 'CARDIOLOGIA', 'ELETIVO', 'URGENTE',
     'Angina estável? Avaliar teste ergométrico', 'AGUARDANDO_AGENDAMENTO', NULL),
    ('d2000000-0000-4000-8000-000000000003', 'd1000000-0000-4000-8000-000000000007', 'PRONTO_ATENDIMENTO', 'IMEDIATO', 'EMERGENCIA',
     'Suspeita de síndrome coronariana aguda', 'PENDENTE', NULL),
    ('d2000000-0000-4000-8000-000000000004', 'd1000000-0000-4000-8000-000000000008', 'ORTOPEDIA', 'ELETIVO', 'URGENTE',
     'Lombociatalgia, avaliar hérnia de disco', 'AGENDADO', NULL),
    ('d2000000-0000-4000-8000-000000000005', 'd1000000-0000-4000-8000-000000000009', 'CARDIOLOGIA', 'ELETIVO', 'POUCO_URGENTE',
     'Investigar arritmia, solicitar Holter', 'AGENDADO', NULL),
    ('d2000000-0000-4000-8000-000000000006', 'd1000000-0000-4000-8000-000000000010', 'CARDIOLOGIA', 'ELETIVO', 'URGENTE',
     'Hipertensão resistente', 'EM_ATENDIMENTO', NULL)
) AS e (id, triagem, especialidade, modalidade, prioridade, justificativa, status, desfecho)
JOIN triagens t ON t.id = e.triagem::uuid
ON CONFLICT DO NOTHING;

INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, triagem_id, encaminhamento_id, profissional_id,
                          duracao_minutos, natureza)
SELECT c.id::uuid, e.paciente_id, c.data_hora, 'CONSULTA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', c.status, e.triagem_id, e.id,
       c.profissional::uuid, 30, c.natureza
FROM (VALUES
    ('d3000000-0000-4000-8000-000000000001', 'd2000000-0000-4000-8000-000000000001', '0c1d2e3f-0000-4000-8000-000000000002',
     (now() AT TIME ZONE 'America/Sao_Paulo')::date - 35 + TIME '09:00', 'INICIAL', 'COMPARECEU'),
    ('d3000000-0000-4000-8000-000000000002', 'd2000000-0000-4000-8000-000000000004', '0c1d2e3f-0000-4000-8000-000000000002',
     (now() AT TIME ZONE 'America/Sao_Paulo')::date + TIME '14:00', 'INICIAL', 'CONFIRMADO'),
    ('d3000000-0000-4000-8000-000000000003', 'd2000000-0000-4000-8000-000000000005', '0c1d2e3f-0000-4000-8000-000000000003',
     (now() AT TIME ZONE 'America/Sao_Paulo')::date + 3 + TIME '10:00', 'INICIAL', 'AGUARDANDO_RESPOSTA'),
    ('d3000000-0000-4000-8000-000000000004', 'd2000000-0000-4000-8000-000000000006', '0c1d2e3f-0000-4000-8000-000000000003',
     date_trunc('hour', now() AT TIME ZONE 'America/Sao_Paulo'), 'RETORNO', 'EM_ANDAMENTO')
) AS c (id, encaminhamento, profissional, data_hora, natureza, status)
JOIN encaminhamentos e ON e.id = c.encaminhamento::uuid
ON CONFLICT DO NOTHING;

INSERT INTO evolucoes_clinicas (agendamento_id, autor_id, texto, registrada_em)
SELECT v.agendamento::uuid, v.autor::uuid, v.texto, v.em
FROM (VALUES
    ('d3000000-0000-4000-8000-000000000001', '0c1d2e3f-0000-4000-8000-000000000002',
     'Crepitação e dor à flexão. RX com redução discreta do espaço articular medial. Fisioterapia 10 sessões.',
     ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 35 + TIME '09:25') AT TIME ZONE 'America/Sao_Paulo'),
    ('d3000000-0000-4000-8000-000000000004', '0c1d2e3f-0000-4000-8000-000000000003',
     'PA 158/96 em uso regular das medicações. Ecocardiograma com hipertrofia ventricular leve.', now())
) AS v (agendamento, autor, texto, em)
WHERE EXISTS (SELECT 1 FROM agendamentos a WHERE a.id = v.agendamento::uuid)
  AND NOT EXISTS (SELECT 1 FROM evolucoes_clinicas x WHERE x.agendamento_id = v.agendamento::uuid);

INSERT INTO retificacoes_triagem (triagem_id, autor_id, texto, justificativa, registrada_em)
SELECT 'd1000000-0000-4000-8000-000000000002', '0c1d2e3f-0000-4000-8000-000000000001', 'Paciente relata também tontura ocasional',
       'Informação lembrada pela paciente após a triagem',
       ((now() AT TIME ZONE 'America/Sao_Paulo')::date - 2 + TIME '11:30') AT TIME ZONE 'America/Sao_Paulo'
WHERE EXISTS (SELECT 1 FROM triagens WHERE id = 'd1000000-0000-4000-8000-000000000002')
  AND NOT EXISTS (SELECT 1 FROM retificacoes_triagem WHERE triagem_id = 'd1000000-0000-4000-8000-000000000002');
