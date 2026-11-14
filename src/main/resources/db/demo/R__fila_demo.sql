CREATE OR REPLACE FUNCTION pg_temp.dia_util(d date) RETURNS date LANGUAGE sql IMMUTABLE AS
$$ SELECT d + CASE extract(isodow FROM d) WHEN 6 THEN 2 WHEN 7 THEN 1 ELSE 0 END $$;

CREATE OR REPLACE FUNCTION pg_temp.demo_id(prefixo text, n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT (prefixo || '000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid $$;

CREATE TEMP TABLE fila_demo ON COMMIT DROP AS
SELECT n,
       pg_temp.demo_id('d4', n) AS paciente_id,
       pg_temp.demo_id('d5', n) AS triagem_id,
       pg_temp.demo_id('d6', n) AS encaminhamento_id,
       pg_temp.demo_id('d8', n) AS entrada_id,
       CASE WHEN n % 2 = 1 THEN 'CARDIOLOGIA' ELSE 'ORTOPEDIA' END AS especialidade,
       CASE WHEN n % 2 = 1 THEN '0c1d2e3f-0000-4000-8000-000000000003'::uuid ELSE '0c1d2e3f-0000-4000-8000-000000000002'::uuid END AS profissional_id,
       (ARRAY ['URGENTE', 'POUCO_URGENTE', 'NAO_URGENTE', 'MUITO_URGENTE', 'POUCO_URGENTE', 'NAO_URGENTE'])[n % 6 + 1] AS prioridade,
       ((((now() AT TIME ZONE 'America/Sao_Paulo')::date - (n % 27) - 3) + TIME '07:30' + n * INTERVAL '11 minutes')
           AT TIME ZONE 'America/Sao_Paulo') AS chegada,
       CASE WHEN n <= 8 THEN 'ACEITOU'
            WHEN n <= 16 THEN 'LIBEROU'
            WHEN n <= 24 THEN 'RECUSOU'
            WHEN n <= 28 THEN 'SEM_CONFIRMACAO'
            WHEN n <= 30 THEN 'PAUSADA'
            WHEN n <= 32 THEN 'OFERTA_ATIVA'
            WHEN n <= 34 THEN 'LIBEROU_AGORA'
            WHEN n <= 36 THEN 'FALTOU'
            ELSE 'CONCLUIDO' END AS grupo,
       (ARRAY ['Helena', 'Otávio', 'Beatriz', 'Gustavo', 'Lívia', 'Rafael', 'Sofia', 'Thiago', 'Marina', 'Eduardo', 'Cecília', 'Bruno',
               'Isabela', 'Leonardo', 'Valentina', 'Diego', 'Camila', 'Felipe', 'Larissa', 'Matheus', 'Gabriela', 'Vinícius', 'Natália',
               'Rodrigo', 'Aline', 'Fernando', 'Patrícia', 'Marcelo', 'Renata', 'André', 'Juliana', 'Ricardo', 'Tatiane', 'Sérgio',
               'Vanessa', 'Paulo', 'Cristina', 'Daniel', 'Elaine', 'Márcio', 'Rosângela', 'Antônio'])[n] || ' ' ||
       (ARRAY ['Oliveira', 'Ferreira', 'Rodrigues', 'Almeida', 'Carvalho', 'Gomes', 'Ribeiro', 'Barbosa', 'Rocha', 'Dias', 'Moreira',
               'Cardoso', 'Teixeira', 'Correia', 'Mendes', 'Freitas', 'Araújo', 'Castro'])[(n * 7) % 18 + 1] AS nome
FROM generate_series(1, 42) AS n;

INSERT INTO pacientes (id, nome_completo, cpf, cartao_sus, data_nascimento, sexo, email, telefone, consentimento)
SELECT f.paciente_id, f.nome, NULL, NULL, DATE '1942-01-01' + (f.n * 557) % 27000,
       CASE WHEN f.n % 2 = 1 THEN 'FEMININO' ELSE 'MASCULINO' END, NULL, '+5551993953661', TRUE
FROM fila_demo f
ON CONFLICT DO NOTHING;

INSERT INTO triagens (id, paciente_id, status, registrada_por, chegada_em, profissional_id, iniciada_em, finalizada_em, queixa_principal,
                      sintomas, pressao_sistolica, pressao_diastolica, frequencia_cardiaca, frequencia_respiratoria, temperatura, saturacao,
                      prioridade)
SELECT f.triagem_id, f.paciente_id, 'FINALIZADA', '0a0b0c0d-0000-4000-8000-000000000002', f.chegada,
       '0c1d2e3f-0000-4000-8000-000000000001', f.chegada + INTERVAL '5 minutes', f.chegada + INTERVAL '20 minutes',
       CASE WHEN f.especialidade = 'CARDIOLOGIA'
            THEN (ARRAY ['Palpitações ao esforço', 'Dor no peito ao caminhar', 'Pressão alta sem controle', 'Falta de ar ao subir escadas'])[f.n % 4 + 1]
            ELSE (ARRAY ['Dor no joelho ao caminhar', 'Dor lombar persistente', 'Dor no ombro ao levantar o braço', 'Dor no quadril ao sentar'])[f.n % 4 + 1] END,
       'Sintomas há algumas semanas, sem sinais de alarme na triagem',
       118 + f.n % 30, 74 + f.n % 16, 68 + f.n % 25, 14 + f.n % 5, 36.2 + (f.n % 7) / 10.0, 95 + f.n % 4, f.prioridade
FROM fila_demo f
WHERE EXISTS (SELECT 1 FROM pacientes p WHERE p.id = f.paciente_id)
ON CONFLICT DO NOTHING;

INSERT INTO encaminhamentos (id, triagem_id, paciente_id, especialidade_codigo, profissional_destino_id, modalidade, prioridade, justificativa,
                             status, criado_por, criado_em, encerrado_em, desfecho)
SELECT f.encaminhamento_id, f.triagem_id, f.paciente_id, f.especialidade, CASE WHEN f.n % 5 = 0 THEN f.profissional_id END, 'ELETIVO',
       f.prioridade,
       CASE WHEN f.especialidade = 'CARDIOLOGIA' THEN 'Avaliação cardiológica eletiva' ELSE 'Avaliação ortopédica eletiva' END,
       CASE WHEN f.grupo = 'CONCLUIDO' THEN 'CONCLUIDO' ELSE 'AGUARDANDO_AGENDAMENTO' END,
       '0c1d2e3f-0000-4000-8000-000000000001', f.chegada + INTERVAL '15 minutes',
       CASE WHEN f.grupo = 'CONCLUIDO' THEN now() - (f.n - 36) * INTERVAL '1 day' END,
       CASE WHEN f.grupo = 'CONCLUIDO' THEN 'Avaliado pelo especialista. Conduta definida e retorno à UBS.' END
FROM fila_demo f
WHERE EXISTS (SELECT 1 FROM triagens t WHERE t.id = f.triagem_id)
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE fila_demo_consultas ON COMMIT DROP AS
SELECT pg_temp.demo_id('d7', 1000 + f.n) AS id, f.n, 'CANCELADO' AS status,
       pg_temp.dia_util((now() AT TIME ZONE 'America/Sao_Paulo')::date + 3 + (f.n - 8)) + TIME '16:30' AS data_hora
FROM fila_demo f WHERE f.grupo = 'LIBEROU'
UNION ALL
SELECT pg_temp.demo_id('d7', 2000 + f.n), f.n, 'CONFIRMADO',
       pg_temp.dia_util((now() AT TIME ZONE 'America/Sao_Paulo')::date + 3 + f.n) + TIME '16:30'
FROM fila_demo f WHERE f.grupo = 'ACEITOU'
UNION ALL
SELECT pg_temp.demo_id('d7', 2000 + f.n), f.n, 'AGUARDANDO_RESPOSTA',
       pg_temp.dia_util((now() AT TIME ZONE 'America/Sao_Paulo')::date + 1) + TIME '11:00' + (f.n - 25) / 2 * INTERVAL '30 minutes'
FROM fila_demo f WHERE f.grupo = 'SEM_CONFIRMACAO'
UNION ALL
SELECT pg_temp.demo_id('d7', 1000 + f.n), f.n, 'CANCELADO',
       pg_temp.dia_util((now() AT TIME ZONE 'America/Sao_Paulo')::date + 2) + TIME '15:30'
FROM fila_demo f WHERE f.grupo = 'LIBEROU_AGORA'
UNION ALL
SELECT pg_temp.demo_id('d7', 3000 + f.n), f.n, 'FALTOU', (now() AT TIME ZONE 'America/Sao_Paulo')::date - (f.n - 30) + TIME '09:00'
FROM fila_demo f WHERE f.grupo = 'FALTOU'
UNION ALL
SELECT pg_temp.demo_id('d7', 3000 + f.n), f.n, 'COMPARECEU', (now() AT TIME ZONE 'America/Sao_Paulo')::date - (f.n - 34) + TIME '10:00'
FROM fila_demo f WHERE f.grupo = 'CONCLUIDO';

INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, triagem_id, encaminhamento_id, profissional_id,
                          duracao_minutos, natureza)
SELECT c.id, f.paciente_id, c.data_hora, 'CONSULTA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', c.status, f.triagem_id,
       f.encaminhamento_id, f.profissional_id, 30, 'INICIAL'
FROM fila_demo_consultas c
JOIN fila_demo f ON f.n = c.n
WHERE EXISTS (SELECT 1 FROM encaminhamentos e WHERE e.id = f.encaminhamento_id)
ON CONFLICT DO NOTHING;

UPDATE encaminhamentos e
SET status = 'AGENDADO'
FROM fila_demo f
WHERE e.id = f.encaminhamento_id AND e.status = 'AGUARDANDO_AGENDAMENTO'
  AND EXISTS (SELECT 1 FROM agendamentos a WHERE a.encaminhamento_id = e.id
                AND a.status IN ('AGENDADO', 'AGUARDANDO_RESPOSTA', 'CONFIRMADO') AND a.data_hora > now() AT TIME ZONE 'America/Sao_Paulo');

INSERT INTO entradas_fila (id, encaminhamento_id, triagem_id, paciente_id, ciclo, especialidade_codigo, profissional_id, prioridade, natureza,
                           status, entrou_em, motivo, atualizada_em)
SELECT f.entrada_id, e.id, e.triagem_id, e.paciente_id, 0, e.especialidade_codigo, e.profissional_destino_id, e.prioridade, 'INICIAL',
       CASE WHEN f.grupo = 'CONCLUIDO' THEN 'ATENDIDA'
            WHEN f.grupo = 'ACEITOU' AND e.status = 'AGENDADO' THEN 'ATENDIDA'
            WHEN f.grupo = 'PAUSADA' THEN 'PAUSADA'
            ELSE 'AGUARDANDO' END,
       t.finalizada_em,
       CASE WHEN f.grupo = 'CONCLUIDO' THEN 'Consulta realizada'
            WHEN f.grupo = 'ACEITOU' AND e.status = 'AGENDADO' THEN 'Vaga liberada aceita'
            WHEN f.n = 29 THEN 'Pessoa internada, aguardar alta para oferecer horário'
            WHEN f.n = 30 THEN 'Sem resposta por telefone, aguardando contato da UBS' END,
       now()
FROM fila_demo f
JOIN encaminhamentos e ON e.id = f.encaminhamento_id
JOIN triagens t ON t.id = e.triagem_id
ON CONFLICT DO NOTHING;

INSERT INTO vagas_liberadas (id, agendamento_origem_id, profissional_id, especialidade_codigo, inicio, duracao_minutos, unidade, endereco,
                             origem, status, motivo, liberada_em, proxima_tentativa_em, encerrada_em)
SELECT pg_temp.demo_id('d9', k), origem.id, origem.profissional_id, f.especialidade, origem.data_hora, 30, origem.unidade, origem.endereco,
       'CANCELAMENTO',
       CASE WHEN nova.id IS NOT NULL THEN 'PREENCHIDA' ELSE 'ENCERRADA' END,
       CASE WHEN nova.id IS NULL THEN 'Horário ocupado na agenda' END,
       now() - (k * 3 + 1) * INTERVAL '1 day', now() - (k * 3 + 1) * INTERVAL '1 day',
       now() - (k * 3 + 1) * INTERVAL '1 day' + INTERVAL '3 hours 25 minutes'
FROM generate_series(1, 8) AS k
JOIN fila_demo f ON f.n = 8 + k
JOIN agendamentos origem ON origem.id = pg_temp.demo_id('d7', 1000 + 8 + k)
LEFT JOIN agendamentos nova ON nova.id = pg_temp.demo_id('d7', 2000 + k) AND nova.status = 'CONFIRMADO'
ON CONFLICT DO NOTHING;

INSERT INTO vagas_liberadas (id, agendamento_origem_id, profissional_id, especialidade_codigo, inicio, duracao_minutos, unidade, endereco,
                             origem, status, liberada_em, proxima_tentativa_em)
SELECT pg_temp.demo_id('d9', 100 + j), origem.id, origem.profissional_id, f.especialidade, origem.data_hora, 30, origem.unidade,
       origem.endereco, 'CANCELAMENTO', 'ABERTA', now() - INTERVAL '40 minutes', now() - INTERVAL '40 minutes'
FROM generate_series(1, 2) AS j
JOIN fila_demo f ON f.n = 32 + j
JOIN agendamentos origem ON origem.id = pg_temp.demo_id('d7', 1000 + 32 + j)
WHERE NOT EXISTS (SELECT 1 FROM agendamentos a
                  WHERE a.profissional_id = origem.profissional_id
                    AND a.status IN ('AGENDADO', 'AGUARDANDO_RESPOSTA', 'CONFIRMADO', 'EM_ANDAMENTO')
                    AND tsrange(a.data_hora, a.data_hora + a.duracao_minutos * INTERVAL '1 minute')
                        && tsrange(origem.data_hora, origem.data_hora + INTERVAL '30 minutes'))
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE fila_demo_ofertas ON COMMIT DROP AS
SELECT pg_temp.demo_id('da', k * 10 + 1) AS id, pg_temp.demo_id('d9', k) AS vaga_id, 16 + k AS n, 'RECUSADA' AS status,
       INTERVAL '5 minutes' AS depois, INTERVAL '40 minutes' AS resposta, CASE WHEN k % 2 = 0 THEN 'PORTAL' ELSE 'WHATSAPP' END AS canal,
       NULL::uuid AS consulta_nova
FROM generate_series(1, 8) AS k
UNION ALL
SELECT pg_temp.demo_id('da', k * 10 + 2), pg_temp.demo_id('d9', k), 16 + (k + 1) % 8 + 1, 'EXPIRADA', INTERVAL '50 minutes', NULL, NULL, NULL
FROM generate_series(1, 8) AS k
UNION ALL
SELECT pg_temp.demo_id('da', k * 10 + 3), pg_temp.demo_id('d9', k), k, 'ACEITA', INTERVAL '2 hours 55 minutes', INTERVAL '25 minutes',
       CASE WHEN k % 2 = 0 THEN 'WHATSAPP' ELSE 'PORTAL' END, pg_temp.demo_id('d7', 2000 + k)
FROM generate_series(1, 8) AS k;

INSERT INTO ofertas_de_vaga (id, vaga_id, entrada_id, paciente_id, status, criada_em, expira_em, envio_tentado_em, enviada_em, id_externo,
                             respondida_em, canal_resposta, motivo, consulta_nova_id)
SELECT o.id, v.id, f.entrada_id, f.paciente_id, o.status, v.liberada_em + o.depois, v.liberada_em + o.depois + INTERVAL '2 hours',
       v.liberada_em + o.depois, v.liberada_em + o.depois + INTERVAL '2 seconds', 'DEMOFILA-OFERTA-' || o.id,
       CASE WHEN o.resposta IS NOT NULL THEN v.liberada_em + o.depois + o.resposta END, o.canal,
       CASE o.status WHEN 'RECUSADA' THEN 'A pessoa não pode neste horário' WHEN 'EXPIRADA' THEN 'Sem resposta no prazo' END,
       o.consulta_nova
FROM fila_demo_ofertas o
JOIN vagas_liberadas v ON v.id = o.vaga_id
JOIN fila_demo f ON f.n = o.n
WHERE EXISTS (SELECT 1 FROM entradas_fila e WHERE e.id = f.entrada_id)
  AND (o.status <> 'ACEITA' OR (v.status = 'PREENCHIDA' AND EXISTS (SELECT 1 FROM agendamentos a WHERE a.id = o.consulta_nova)))
ON CONFLICT DO NOTHING;

INSERT INTO ofertas_de_vaga (id, vaga_id, entrada_id, paciente_id, status, criada_em, expira_em, envio_tentado_em, enviada_em, id_externo)
SELECT pg_temp.demo_id('da', 1000 + j), v.id, f.entrada_id, f.paciente_id, 'ENVIADA', now() - INTERVAL '35 minutes',
       now() + INTERVAL '85 minutes', now() - INTERVAL '35 minutes', now() - INTERVAL '35 minutes', 'DEMOFILA-OFERTA-ATIVA-' || j
FROM generate_series(1, 2) AS j
JOIN vagas_liberadas v ON v.id = pg_temp.demo_id('d9', 100 + j) AND v.status = 'ABERTA'
JOIN fila_demo f ON f.n = 30 + j
JOIN entradas_fila e ON e.id = f.entrada_id AND e.status = 'AGUARDANDO'
ON CONFLICT DO NOTHING;

INSERT INTO registros_mensagem (agendamento_id, destino, chave_destino, tipo, texto, sucesso, id_externo, enviado_em, entregue_em)
SELECT a.id, '+5551993953661', '5193953661', 'PEDIDO_CONFIRMACAO', 'Pedido de confirmação de presença (dados de demonstração)', TRUE,
       'DEMOFILA-PEDIDO-' || f.n,
       LEAST(now() - INTERVAL '20 hours', (a.data_hora - INTERVAL '3 days') AT TIME ZONE 'America/Sao_Paulo'),
       CASE WHEN f.n NOT IN (27, 35) THEN LEAST(now() - INTERVAL '20 hours', (a.data_hora - INTERVAL '3 days') AT TIME ZONE 'America/Sao_Paulo')
                                         + INTERVAL '4 seconds' END
FROM fila_demo f
JOIN agendamentos a ON a.encaminhamento_id = f.encaminhamento_id AND a.status IN ('AGUARDANDO_RESPOSTA', 'FALTOU', 'COMPARECEU')
WHERE f.grupo IN ('SEM_CONFIRMACAO', 'FALTOU', 'CONCLUIDO')
  AND NOT EXISTS (SELECT 1 FROM registros_mensagem r WHERE r.agendamento_id = a.id AND r.tipo = 'PEDIDO_CONFIRMACAO');

INSERT INTO mensagens_recebidas (agendamento_id, remetente, texto, id_externo, resposta, recebida_em)
SELECT a.id, '+5551993953661', '1', 'DEMOFILA-RESPOSTA-' || f.n, 'CONFIRMAR', r.enviado_em + INTERVAL '2 hours'
FROM fila_demo f
JOIN agendamentos a ON a.encaminhamento_id = f.encaminhamento_id AND a.status = 'COMPARECEU'
JOIN registros_mensagem r ON r.agendamento_id = a.id AND r.tipo = 'PEDIDO_CONFIRMACAO'
WHERE f.n BETWEEN 37 AND 41
  AND NOT EXISTS (SELECT 1 FROM mensagens_recebidas m WHERE m.agendamento_id = a.id);

INSERT INTO confirmacoes_de_presenca (agendamento_id, decisao, decidida_em, papel)
SELECT a.id, CASE f.n WHEN 27 THEN 'SEM_CONTATO' ELSE 'MANTIDA_SEM_RESPOSTA' END, now() - INTERVAL '2 hours', 'SISTEMA'
FROM fila_demo f
JOIN agendamentos a ON a.encaminhamento_id = f.encaminhamento_id AND a.status = 'AGUARDANDO_RESPOSTA'
WHERE f.n IN (27, 28)
ON CONFLICT DO NOTHING;

INSERT INTO movimentacoes_fila (entrada_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT e.id, e.paciente_id, 'ENTROU_NA_FILA', 'Encaminhamento liberado para agendamento (inicial)', 'SISTEMA', e.entrou_em
FROM entradas_fila e JOIN fila_demo f ON f.entrada_id = e.id
WHERE NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.entrada_id = e.id AND m.evento = 'ENTROU_NA_FILA');

INSERT INTO movimentacoes_fila (entrada_id, paciente_id, evento, detalhe, ator_id, papel, registrada_em)
SELECT e.id, e.paciente_id, 'PAUSADA', e.motivo, '0a0b0c0d-0000-4000-8000-000000000002', 'RECEPCAO', e.entrou_em + INTERVAL '2 days'
FROM entradas_fila e JOIN fila_demo f ON f.entrada_id = e.id
WHERE e.status = 'PAUSADA'
  AND NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.entrada_id = e.id AND m.evento = 'PAUSADA');

INSERT INTO movimentacoes_fila (entrada_id, agendamento_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT e.id, a.id, e.paciente_id, 'SAIU_DA_FILA', 'Consulta realizada', 'SISTEMA', a.data_hora AT TIME ZONE 'America/Sao_Paulo'
FROM entradas_fila e
JOIN fila_demo f ON f.entrada_id = e.id AND f.grupo = 'CONCLUIDO'
JOIN agendamentos a ON a.encaminhamento_id = e.encaminhamento_id AND a.status = 'COMPARECEU'
WHERE NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.entrada_id = e.id AND m.evento = 'SAIU_DA_FILA');

INSERT INTO movimentacoes_fila (vaga_id, agendamento_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT v.id, v.agendamento_origem_id, a.paciente_id, 'VAGA_LIBERADA', 'Consulta cancelada ou remarcada', 'SISTEMA', v.liberada_em
FROM vagas_liberadas v JOIN agendamentos a ON a.id = v.agendamento_origem_id
WHERE v.id IN (SELECT pg_temp.demo_id('d9', k) FROM generate_series(1, 8) AS k UNION ALL SELECT pg_temp.demo_id('d9', 100 + j) FROM generate_series(1, 2) AS j)
  AND NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.vaga_id = v.id AND m.evento = 'VAGA_LIBERADA');

INSERT INTO movimentacoes_fila (entrada_id, vaga_id, agendamento_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT e.id, v.id, v.agendamento_origem_id, e.paciente_id, 'RETOMADA',
       'A consulta foi desmarcada; volta para a fila com a data de entrada original', 'SISTEMA', v.liberada_em
FROM vagas_liberadas v
JOIN agendamentos a ON a.id = v.agendamento_origem_id
JOIN entradas_fila e ON e.encaminhamento_id = a.encaminhamento_id AND e.ciclo = 0
WHERE v.id IN (SELECT pg_temp.demo_id('d9', k) FROM generate_series(1, 8) AS k UNION ALL SELECT pg_temp.demo_id('d9', 100 + j) FROM generate_series(1, 2) AS j)
  AND NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.entrada_id = e.id AND m.evento = 'RETOMADA');

INSERT INTO movimentacoes_fila (entrada_id, oferta_id, vaga_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT o.entrada_id, o.id, o.vaga_id, o.paciente_id, ev.evento, ev.detalhe, 'SISTEMA', ev.em
FROM ofertas_de_vaga o
CROSS JOIN LATERAL (VALUES
    ('OFERTA_CRIADA', 'Vaga oferecida à próxima pessoa elegível da fila', o.criada_em),
    ('OFERTA_ENVIADA', 'Oferta enviada pelo WhatsApp e disponível no portal', o.enviada_em),
    (CASE o.status WHEN 'ACEITA' THEN 'OFERTA_ACEITA' WHEN 'RECUSADA' THEN 'OFERTA_RECUSADA' WHEN 'EXPIRADA' THEN 'OFERTA_EXPIRADA' END,
     CASE o.status WHEN 'ACEITA' THEN 'Vaga aceita e consulta confirmada'
                   WHEN 'RECUSADA' THEN 'A pessoa não pode neste horário e continua na fila na mesma posição'
                   WHEN 'EXPIRADA' THEN 'Sem resposta no prazo. A pessoa continua na fila na mesma posição.' END,
     COALESCE(o.respondida_em, o.expira_em))
) AS ev (evento, detalhe, em)
WHERE o.id_externo LIKE 'DEMOFILA-OFERTA-%' AND ev.evento IS NOT NULL AND ev.em <= now()
  AND NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.oferta_id = o.id AND m.evento = ev.evento);

CREATE TEMP TABLE fila_demo_demais ON COMMIT DROP AS
SELECT p.id AS paciente_id,
       md5(p.id::text || ':fila-demo:triagem')::uuid AS triagem_id,
       md5(p.id::text || ':fila-demo:encaminhamento')::uuid AS encaminhamento_id,
       CASE WHEN get_byte(decode(md5(p.id::text), 'hex'), 0) % 2 = 0 THEN 'ORTOPEDIA' ELSE 'CARDIOLOGIA' END AS especialidade,
       (ARRAY ['POUCO_URGENTE', 'NAO_URGENTE', 'URGENTE'])[get_byte(decode(md5(p.id::text), 'hex'), 1) % 3 + 1] AS prioridade,
       ((((now() AT TIME ZONE 'America/Sao_Paulo')::date - (2 + get_byte(decode(md5(p.id::text), 'hex'), 2) % 20)) + TIME '08:15')
           AT TIME ZONE 'America/Sao_Paulo') AS chegada
FROM pacientes p
WHERE p.id NOT IN (SELECT paciente_id FROM fila_demo)
  AND NOT EXISTS (SELECT 1 FROM encaminhamentos e JOIN triagens t ON t.id = e.triagem_id AND t.status = 'FINALIZADA'
                  WHERE e.paciente_id = p.id AND e.modalidade = 'ELETIVO' AND e.status IN ('AGUARDANDO_AGENDAMENTO', 'AGENDADO'));

INSERT INTO triagens (id, paciente_id, status, registrada_por, chegada_em, profissional_id, iniciada_em, finalizada_em, queixa_principal,
                      sintomas, pressao_sistolica, pressao_diastolica, frequencia_cardiaca, frequencia_respiratoria, temperatura, saturacao,
                      prioridade)
SELECT d.triagem_id, d.paciente_id, 'FINALIZADA', '0a0b0c0d-0000-4000-8000-000000000002', d.chegada,
       '0c1d2e3f-0000-4000-8000-000000000001', d.chegada + INTERVAL '5 minutes', d.chegada + INTERVAL '20 minutes',
       CASE WHEN d.especialidade = 'CARDIOLOGIA' THEN 'Cansaço aos esforços' ELSE 'Dor articular ao caminhar' END,
       'Queixa crônica, estável na triagem', 124, 80, 76, 16, 36.5, 97, d.prioridade
FROM fila_demo_demais d
ON CONFLICT DO NOTHING;

INSERT INTO encaminhamentos (id, triagem_id, paciente_id, especialidade_codigo, modalidade, prioridade, justificativa, status, criado_por,
                             criado_em)
SELECT d.encaminhamento_id, d.triagem_id, d.paciente_id, d.especialidade, 'ELETIVO', d.prioridade,
       CASE WHEN d.especialidade = 'CARDIOLOGIA' THEN 'Avaliação cardiológica eletiva' ELSE 'Avaliação ortopédica eletiva' END,
       'AGUARDANDO_AGENDAMENTO', '0c1d2e3f-0000-4000-8000-000000000001', d.chegada + INTERVAL '15 minutes'
FROM fila_demo_demais d
WHERE EXISTS (SELECT 1 FROM triagens t WHERE t.id = d.triagem_id)
ON CONFLICT DO NOTHING;

INSERT INTO entradas_fila (id, encaminhamento_id, triagem_id, paciente_id, ciclo, especialidade_codigo, profissional_id, prioridade, natureza,
                           status, entrou_em, atualizada_em)
SELECT gen_random_uuid(), enc.id, enc.triagem_id, enc.paciente_id, c.ciclo, enc.especialidade_codigo, enc.profissional_destino_id,
       enc.prioridade, CASE WHEN c.ciclo = 0 THEN 'INICIAL' ELSE 'RETORNO' END, 'AGUARDANDO',
       CASE WHEN c.ciclo = 0 THEN coalesce(t.finalizada_em, now()) ELSE now() END, now()
FROM encaminhamentos enc
JOIN triagens t ON t.id = enc.triagem_id AND t.status = 'FINALIZADA'
CROSS JOIN LATERAL (SELECT count(*)::int AS ciclo FROM agendamentos a WHERE a.encaminhamento_id = enc.id AND a.status = 'COMPARECEU') c
WHERE enc.modalidade = 'ELETIVO' AND enc.status IN ('AGUARDANDO_AGENDAMENTO', 'AGENDADO')
ON CONFLICT (encaminhamento_id, ciclo) DO NOTHING;

INSERT INTO movimentacoes_fila (entrada_id, paciente_id, evento, detalhe, papel, registrada_em)
SELECT e.id, e.paciente_id, 'ENTROU_NA_FILA', 'Encaminhamento liberado para agendamento (' || lower(e.natureza) || ')', 'SISTEMA', e.entrou_em
FROM entradas_fila e
WHERE NOT EXISTS (SELECT 1 FROM movimentacoes_fila m WHERE m.entrada_id = e.id AND m.evento = 'ENTROU_NA_FILA');
