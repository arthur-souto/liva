\set ON_ERROR_STOP 1
BEGIN;

CREATE TEMP TABLE rosa_consultas AS SELECT id FROM agendamentos WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
CREATE TEMP TABLE rosa_vagas AS SELECT id FROM vagas_liberadas WHERE agendamento_origem_id IN (SELECT id FROM rosa_consultas);

DELETE FROM ofertas_de_vaga WHERE vaga_id IN (SELECT id FROM rosa_vagas) OR paciente_id = 'a9e5e000-0000-4000-8000-000000000001'
    OR consulta_nova_id IN (SELECT id FROM rosa_consultas) OR consulta_anterior_id IN (SELECT id FROM rosa_consultas);
DELETE FROM movimentacoes_fila WHERE vaga_id IN (SELECT id FROM rosa_vagas) OR agendamento_id IN (SELECT id FROM rosa_consultas)
    OR paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM vagas_liberadas WHERE id IN (SELECT id FROM rosa_vagas);
DELETE FROM registros_mensagem WHERE agendamento_id IN (SELECT id FROM rosa_consultas);
DELETE FROM mensagens_recebidas WHERE agendamento_id IN (SELECT id FROM rosa_consultas);
DELETE FROM confirmacoes_de_presenca WHERE agendamento_id IN (SELECT id FROM rosa_consultas);
DELETE FROM motivos_de_ausencia WHERE agendamento_id IN (SELECT id FROM rosa_consultas) OR paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM ligacoes_de_prevencao WHERE agendamento_id IN (SELECT id FROM rosa_consultas);
DELETE FROM remarcacoes WHERE consulta_nova_id IN (SELECT id FROM rosa_consultas) OR consulta_anterior_id IN (SELECT id FROM rosa_consultas)
    OR paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM evolucoes_clinicas WHERE agendamento_id IN (SELECT id FROM rosa_consultas);
DELETE FROM conversas_whatsapp WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM links_de_acesso WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM codigos_portal WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM vinculos_de_cuidado WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM usuarios WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM agendamentos WHERE id IN (SELECT id FROM rosa_consultas);
DELETE FROM encaminhamentos WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM triagens WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001';
DELETE FROM pacientes WHERE id = 'a9e5e000-0000-4000-8000-000000000001';

INSERT INTO pacientes (id, nome_completo, cpf, data_nascimento, sexo, telefone, cep, logradouro, numero, bairro, cidade, uf, consentimento)
VALUES ('a9e5e000-0000-4000-8000-000000000001', 'Rosa Oliveira', '15881399803', DATE '1961-05-20', 'FEMININO', :'telefone',
        '01001000', 'Rua das Flores', '120', 'Vila Nova', 'São Paulo', 'SP', TRUE);

INSERT INTO usuarios (id, login, nome, senha_hash, papel, profissional_id, paciente_id, criado_em)
SELECT 'a9e5e000-0000-4000-8000-000000000002', '15881399803', 'Rosa Oliveira', senha_hash, 'PACIENTE', NULL,
       'a9e5e000-0000-4000-8000-000000000001', now()
FROM usuarios WHERE login = 'admin@liva.demo';

INSERT INTO triagens (id, paciente_id, status, registrada_por, chegada_em, profissional_id, iniciada_em, finalizada_em,
                      queixa_principal, prioridade)
VALUES ('a9e5e000-0000-4000-8000-000000000011', 'a9e5e000-0000-4000-8000-000000000001', 'FINALIZADA', '0a0b0c0d-0000-4000-8000-000000000002',
        now() - INTERVAL '20 days', '0c1d2e3f-0000-4000-8000-000000000001', now() - INTERVAL '20 days' + INTERVAL '10 minutes',
        now() - INTERVAL '20 days' + INTERVAL '25 minutes', 'Dor no joelho direito há 3 meses, pior ao subir escadas', 'POUCO_URGENTE');

INSERT INTO encaminhamentos (id, triagem_id, paciente_id, especialidade_codigo, modalidade, prioridade, justificativa, status, criado_por, criado_em)
VALUES ('a9e5e000-0000-4000-8000-000000000021', 'a9e5e000-0000-4000-8000-000000000011', 'a9e5e000-0000-4000-8000-000000000001',
        'ORTOPEDIA', 'ELETIVO', 'POUCO_URGENTE', 'Gonalgia crônica, avaliar artrose', 'AGENDADO',
        '0c1d2e3f-0000-4000-8000-000000000001', now() - INTERVAL '20 days' + INTERVAL '25 minutes');

INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, profissional_id, duracao_minutos, natureza)
SELECT h.id::uuid, 'a9e5e000-0000-4000-8000-000000000001', (now() AT TIME ZONE 'America/Sao_Paulo')::date - h.dias + h.hora,
       'CONSULTA', p.unidade, p.endereco, h.status, p.id, 30, 'AVULSA'
FROM (VALUES
    ('a9e5e000-0000-4000-8000-000000000031', 75, TIME '07:10', 'FALTOU'),
    ('a9e5e000-0000-4000-8000-000000000032', 41, TIME '07:10', 'FALTOU'),
    ('a9e5e000-0000-4000-8000-000000000033', 27, TIME '07:10', 'COMPARECEU')
) AS h (id, dias, hora, status)
JOIN profissionais p ON p.id = '0c1d2e3f-0000-4000-8000-000000000003';

INSERT INTO motivos_de_ausencia (agendamento_id, paciente_id, ocorrencia, motivo, canal, perguntado_em, respondido_em)
VALUES ('a9e5e000-0000-4000-8000-000000000031', 'a9e5e000-0000-4000-8000-000000000001', 'FALTA', 'TRANSPORTE', 'WHATSAPP',
        now() - INTERVAL '75 days' + INTERVAL '4 hours', now() - INTERVAL '75 days' + INTERVAL '5 hours'),
       ('a9e5e000-0000-4000-8000-000000000032', 'a9e5e000-0000-4000-8000-000000000001', 'FALTA', 'TRABALHO', 'WHATSAPP',
        now() - INTERVAL '41 days' + INTERVAL '4 hours', now() - INTERVAL '41 days' + INTERVAL '6 hours');

DO $$
DECLARE
    agora TIMESTAMP := date_trunc('minute', now() AT TIME ZONE 'America/Sao_Paulo');
    tentativa INTEGER;
    horario TIMESTAMP;
BEGIN
    FOR tentativa IN 0..40 LOOP
        horario := agora - INTERVAL '45 minutes' - tentativa * INTERVAL '10 minutes';
        BEGIN
            INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, triagem_id, encaminhamento_id, profissional_id,
                                      duracao_minutos, natureza)
            SELECT 'a9e5e000-0000-4000-8000-000000000041', 'a9e5e000-0000-4000-8000-000000000001', horario, 'CONSULTA', p.unidade, p.endereco,
                   'AGENDADO', 'a9e5e000-0000-4000-8000-000000000011', 'a9e5e000-0000-4000-8000-000000000021', p.id, 30, 'INICIAL'
            FROM profissionais p WHERE p.id = '0c1d2e3f-0000-4000-8000-000000000002';
            EXIT;
        EXCEPTION WHEN exclusion_violation THEN
            NULL;
        END;
    END LOOP;

    FOR tentativa IN 0..40 LOOP
        horario := (agora::date + 2) + TIME '09:20' + tentativa * INTERVAL '10 minutes';
        BEGIN
            INSERT INTO agendamentos (id, paciente_id, data_hora, tipo, unidade, endereco, status, profissional_id, duracao_minutos, natureza)
            SELECT 'a9e5e000-0000-4000-8000-000000000042', 'a9e5e000-0000-4000-8000-000000000001', horario, 'CONSULTA', p.unidade, p.endereco,
                   'AGUARDANDO_RESPOSTA', p.id, 30, 'AVULSA'
            FROM profissionais p WHERE p.id = '0c1d2e3f-0000-4000-8000-000000000003';
            EXIT;
        EXCEPTION WHEN exclusion_violation THEN
            NULL;
        END;
    END LOOP;
END $$;

INSERT INTO registros_mensagem (agendamento_id, destino, chave_destino, tipo, texto, sucesso, enviado_em, entregue_em)
VALUES ('a9e5e000-0000-4000-8000-000000000042', :'telefone', :'chave', 'CONFIRMACAO_AGENDAMENTO',
        'Olá, Rosa Oliveira! Seu atendimento na Policlínica Central foi agendado.', TRUE, now() - INTERVAL '35 days', now() - INTERVAL '35 days'),
       ('a9e5e000-0000-4000-8000-000000000042', :'telefone', :'chave', 'PEDIDO_CONFIRMACAO',
        'Olá, Rosa Oliveira! Você tem atendimento na Policlínica Central. Você consegue ir?', TRUE, now() - INTERVAL '1 minute',
        now() - INTERVAL '1 minute');

COMMIT;

SELECT nome_completo AS paciente, cpf AS login_do_portal FROM pacientes WHERE id = 'a9e5e000-0000-4000-8000-000000000001';
SELECT to_char(data_hora, 'DD/MM HH24:MI') AS horario, status, natureza,
       CASE id WHEN 'a9e5e000-0000-4000-8000-000000000041' THEN 'registrar a falta (ortopedia@liva.demo)'
               WHEN 'a9e5e000-0000-4000-8000-000000000042' THEN 'primeira da lista Ligar primeiro'
               ELSE 'histórico' END AS papel_na_demo
FROM agendamentos WHERE paciente_id = 'a9e5e000-0000-4000-8000-000000000001' ORDER BY data_hora;
SELECT CASE WHEN count(*) = 0 THEN 'ok: o pedido de confirmação da Rosa é o mais recente deste WhatsApp'
            ELSE 'ATENÇÃO: há ' || count(*) || ' pedido(s) de confirmação mais recente(s) para este WhatsApp; responda-os antes da demonstração' END AS verificacao
FROM registros_mensagem r
JOIN agendamentos a ON a.id = r.agendamento_id
WHERE r.chave_destino = :'chave' AND r.tipo = 'PEDIDO_CONFIRMACAO' AND a.status = 'AGUARDANDO_RESPOSTA'
  AND a.paciente_id <> 'a9e5e000-0000-4000-8000-000000000001'
  AND r.enviado_em > (SELECT max(enviado_em) FROM registros_mensagem WHERE agendamento_id = 'a9e5e000-0000-4000-8000-000000000042');
