CREATE OR REPLACE FUNCTION pg_temp.demo_id(prefixo text, n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT (prefixo || '000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid $$;

CREATE OR REPLACE FUNCTION pg_temp.sorteio(chave text) RETURNS double precision LANGUAGE sql IMMUTABLE AS
$$ SELECT ((hashtext(chave)::bigint & 1048575) / 1048576.0)::double precision $$;

CREATE TEMP TABLE faltas_pacientes ON COMMIT DROP AS
SELECT id, row_number() OVER (ORDER BY id) - 1 AS ordem
FROM pacientes
WHERE id <> '918d6bf7-a836-4de4-a442-0e6a16a1703c';

CREATE TEMP TABLE faltas_faltosos ON COMMIT DROP AS
SELECT id, row_number() OVER (ORDER BY id) - 1 AS ordem
FROM faltas_pacientes
WHERE ordem % 4 = 0;

CREATE TEMP TABLE faltas_consultas ON COMMIT DROP AS
WITH base AS (
    SELECT w, i,
           date_trunc('week', (now() AT TIME ZONE 'America/Sao_Paulo'))::date - w * 7 AS segunda,
           pg_temp.sorteio('situacao-' || w || '-' || i) AS r
    FROM generate_series(1, 12) w, generate_series(0, 29) i
), taxas AS (
    SELECT b.*,
           CASE WHEN w >= 7 THEN 0.27 ELSE (ARRAY [0.09, 0.10, 0.12, 0.15, 0.18, 0.22])[w] END AS p_falta,
           CASE WHEN w >= 7 THEN 0.08 ELSE (ARRAY [0.17, 0.16, 0.15, 0.13, 0.12, 0.10])[w] END AS p_cancelamento
    FROM base b
)
SELECT w, i,
       pg_temp.demo_id('e1', w * 100 + i) AS id,
       (segunda + (i % 5)) + TIME '08:00' + ((i / 5) % 6) * INTERVAL '90 minutes' + (i % 2) * INTERVAL '30 minutes' AS data_hora,
       CASE WHEN i % 2 = 0 THEN '0c1d2e3f-0000-4000-8000-000000000002'::uuid ELSE '0c1d2e3f-0000-4000-8000-000000000003'::uuid END AS profissional_id,
       CASE WHEN r < p_falta THEN 'FALTOU' WHEN r < p_falta + p_cancelamento THEN 'CANCELADO' ELSE 'COMPARECEU' END AS status,
       r
FROM taxas;

INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, profissional_id, duracao_minutos, natureza)
SELECT c.id,
       CASE WHEN c.status = 'FALTOU'
            THEN (SELECT f.id FROM faltas_faltosos f WHERE f.ordem = (c.w * 7 + c.i) % (SELECT count(*) FROM faltas_faltosos))
            ELSE (SELECT p.id FROM faltas_pacientes p WHERE p.ordem = (c.w * 31 + c.i * 3) % (SELECT count(*) FROM faltas_pacientes)) END,
       c.data_hora, 'CONSULTA', pr.unidade, pr.endereco, c.status, c.profissional_id, 30, 'AVULSA'
FROM faltas_consultas c
JOIN profissionais pr ON pr.id = c.profissional_id
WHERE (SELECT count(*) FROM faltas_faltosos) > 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO vagas_liberadas (id, agendamento_origem_id, profissional_id, especialidade_codigo, inicio, duracao_minutos, unidade, endereco,
                             origem, status, motivo, liberada_em, proxima_tentativa_em, encerrada_em)
SELECT pg_temp.demo_id('e2', c.w * 100 + c.i), a.id, a.profissional_id, pr.especialidade_codigo, a.data_hora, a.duracao_minutos,
       a.unidade, a.endereco, 'CANCELAMENTO', 'PREENCHIDA', 'Ocupada por quem estava na fila',
       (a.data_hora - INTERVAL '3 days') AT TIME ZONE 'America/Sao_Paulo',
       (a.data_hora - INTERVAL '3 days') AT TIME ZONE 'America/Sao_Paulo',
       (a.data_hora - INTERVAL '2 days 20 hours') AT TIME ZONE 'America/Sao_Paulo'
FROM faltas_consultas c
JOIN agendamentos a ON a.id = c.id AND a.status = 'CANCELADO'
JOIN profissionais pr ON pr.id = a.profissional_id
WHERE c.w <= 6 AND pg_temp.sorteio('vaga-' || c.w || '-' || c.i) < 0.7
ON CONFLICT DO NOTHING;

INSERT INTO motivos_de_ausencia (agendamento_id, paciente_id, ocorrencia, motivo, canal, perguntado_em, respondido_em)
SELECT a.id, a.paciente_id,
       CASE a.status WHEN 'FALTOU' THEN 'FALTA' ELSE 'CANCELAMENTO' END,
       m.motivo,
       CASE WHEN m.motivo IS NULL THEN NULL WHEN pg_temp.sorteio('canal-' || a.id) < 0.8 THEN 'WHATSAPP' ELSE 'PORTAL' END,
       (a.data_hora + CASE a.status WHEN 'FALTOU' THEN INTERVAL '2 hours' ELSE INTERVAL '-2 days' END) AT TIME ZONE 'America/Sao_Paulo',
       CASE WHEN m.motivo IS NULL THEN NULL
            ELSE (a.data_hora + CASE a.status WHEN 'FALTOU' THEN INTERVAL '4 hours' ELSE INTERVAL '-47 hours' END) AT TIME ZONE 'America/Sao_Paulo' END
FROM faltas_consultas c
JOIN agendamentos a ON a.id = c.id AND a.status IN ('FALTOU', 'CANCELADO')
CROSS JOIN LATERAL (
    SELECT CASE
               WHEN pg_temp.sorteio('responde-' || a.id) >= CASE WHEN c.w <= 6 THEN 0.78 ELSE 0.6 END THEN NULL
               WHEN a.status = 'FALTOU' THEN
                   CASE WHEN s < 0.40 - CASE WHEN c.w <= 6 THEN 0.12 ELSE 0 END THEN 'ESQUECI'
                        WHEN s < 0.60 THEN 'TRANSPORTE'
                        WHEN s < 0.78 THEN 'TRABALHO'
                        WHEN s < 0.90 THEN 'SAUDE'
                        WHEN s < 0.95 THEN 'NAO_PRECISA'
                        ELSE 'OUTRO' END
               ELSE
                   CASE WHEN s < 0.30 THEN 'TRABALHO'
                        WHEN s < 0.55 THEN 'SAUDE'
                        WHEN s < 0.70 THEN 'TRANSPORTE'
                        WHEN s < 0.85 THEN 'NAO_PRECISA'
                        WHEN s < 0.90 THEN 'ESQUECI'
                        ELSE 'OUTRO' END
               END AS motivo
    FROM (SELECT pg_temp.sorteio('motivo-' || a.id) AS s) x
) m
ON CONFLICT (agendamento_id) DO NOTHING;
