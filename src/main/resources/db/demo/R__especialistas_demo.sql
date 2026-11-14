INSERT INTO profissionais (id, nome, registro_conselho, tipo, especialidade_codigo, unidade, endereco, ativo) VALUES
    ('0c1d2e3f-0000-4000-8000-000000000005', 'Dr. Rafael Souza', 'CRM-SP 100005', 'ESPECIALISTA', 'NEUROLOGIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000006', 'Dra. Mariana Alves', 'CRM-SP 100006', 'ESPECIALISTA', 'DERMATOLOGIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000007', 'Dra. Helena Prado', 'CRM-SP 100007', 'ESPECIALISTA', 'GINECOLOGIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE),
    ('0c1d2e3f-0000-4000-8000-000000000008', 'Dr. Tiago Martins', 'CRM-SP 100008', 'ESPECIALISTA', 'PEDIATRIA', 'Policlínica Central', 'Av. Brasil, 500 - Centro', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO disponibilidades (id, profissional_id, dia_semana, hora_inicio, hora_fim, duracao_minutos)
SELECT gen_random_uuid(), a.profissional, a.dia, t.inicio, t.fim, 30
FROM (VALUES
    ('0c1d2e3f-0000-4000-8000-000000000005'::uuid, 2), ('0c1d2e3f-0000-4000-8000-000000000005'::uuid, 3), ('0c1d2e3f-0000-4000-8000-000000000005'::uuid, 4),
    ('0c1d2e3f-0000-4000-8000-000000000006'::uuid, 1), ('0c1d2e3f-0000-4000-8000-000000000006'::uuid, 3), ('0c1d2e3f-0000-4000-8000-000000000006'::uuid, 5),
    ('0c1d2e3f-0000-4000-8000-000000000007'::uuid, 1), ('0c1d2e3f-0000-4000-8000-000000000007'::uuid, 2), ('0c1d2e3f-0000-4000-8000-000000000007'::uuid, 4),
    ('0c1d2e3f-0000-4000-8000-000000000008'::uuid, 1), ('0c1d2e3f-0000-4000-8000-000000000008'::uuid, 3), ('0c1d2e3f-0000-4000-8000-000000000008'::uuid, 5)
) AS a (profissional, dia)
CROSS JOIN (VALUES (TIME '08:00', TIME '12:00'), (TIME '13:30', TIME '17:00')) AS t (inicio, fim)
WHERE NOT EXISTS (SELECT 1 FROM disponibilidades x WHERE x.profissional_id = a.profissional AND x.dia_semana = a.dia AND x.hora_inicio = t.inicio);

INSERT INTO usuarios (id, login, nome, senha_hash, papel, profissional_id, paciente_id, criado_em)
SELECT n.id, n.login, n.nome, a.senha_hash, 'ESPECIALISTA', n.profissional, NULL, now()
FROM (VALUES
    ('0a0b0c0d-0000-4000-8000-000000000011'::uuid, 'neurologia@liva.demo', 'Dr. Rafael Souza', '0c1d2e3f-0000-4000-8000-000000000005'::uuid),
    ('0a0b0c0d-0000-4000-8000-000000000012'::uuid, 'dermatologia@liva.demo', 'Dra. Mariana Alves', '0c1d2e3f-0000-4000-8000-000000000006'::uuid),
    ('0a0b0c0d-0000-4000-8000-000000000013'::uuid, 'ginecologia@liva.demo', 'Dra. Helena Prado', '0c1d2e3f-0000-4000-8000-000000000007'::uuid),
    ('0a0b0c0d-0000-4000-8000-000000000014'::uuid, 'pediatria@liva.demo', 'Dr. Tiago Martins', '0c1d2e3f-0000-4000-8000-000000000008'::uuid)
) AS n (id, login, nome, profissional)
JOIN usuarios a ON a.login = 'admin@liva.demo'
ON CONFLICT DO NOTHING;
