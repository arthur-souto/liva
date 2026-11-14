CREATE UNIQUE INDEX uk_pacientes_cpf ON pacientes (cpf) WHERE cpf IS NOT NULL;
CREATE INDEX idx_pacientes_telefone ON pacientes (telefone);
