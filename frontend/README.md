# Liva · Front-end

Painel da equipe da unidade de saúde para agendar atendimentos, acompanhar as respostas dos pacientes no WhatsApp e agir sobre cancelamentos e pedidos de remarcação.

Next.js 16 (App Router) + React 19 + TypeScript, organizado por feature, com o design system centralizado em `src/shared/styles/token.css`.

## Como rodar

1. Suba o backend (porta 8081) a partir da raiz do repositório.
2. Configure o front:

   ```bash
   cp .env.example .env.local
   ```

   Só é preciso `LIVA_API_URL`. A API key não é mais usada pelo front: cada pessoa entra com o próprio usuário (veja "Usuários de demonstração" no README da raiz).
3. Instale e rode:

   ```bash
   npm install
   npm run dev
   ```

4. Abra http://localhost:3000 e entre com um usuário.

| Script | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento na porta 3000 |
| `npm run typecheck` | Checagem de tipos |

## Sessão e proxy (BFF)

O navegador nunca vê o token da API.

- `/api/sessao/entrar` (e `/api/sessao/portal/ativar`) chama o backend e guarda o token no cookie httpOnly `liva_sessao`. O corpo devolvido traz só os dados do usuário.
- Toda chamada do front vai para `/api/liva/*`, um Route Handler que lê o cookie, envia `Authorization: Bearer` e repassa para `LIVA_API_URL`.
- Escritas vindas de outra origem são recusadas (403). `/api/auth/**` e o webhook da Evolution não são expostos pelo proxy.
- Um `401` do backend apaga o cookie, e a tela volta para `/entrar`.
- `src/proxy.ts` redireciona quem está sem sessão (ou com token expirado) para `/entrar?volta=...` e manda cada papel para a sua área. Ele só lê o papel do token para rotear; quem valida a assinatura e as permissões é o backend.
- `DELETE /api/sessao` encerra a sessão.
- `/api/liva-health` consulta `/actuator/health` para o indicador de status do menu.

## Estrutura

```
src/
  app/                 rotas (páginas finas) e Route Handlers do proxy
  features/
    painel/            início: próximo atendimento, contadores, rotina diária
    agendar/           fluxo em 2 passos e resultado dos envios
    calendario/        calendário mensal com os atendimentos do dia
    conversas/         simulador de respostas do paciente e envio avulso
    avisos/            cancelamentos, remarcações e quem ainda não respondeu
    agendamentos/      lista com status e resposta, detalhe, tipos, API e disparo manual
    acesso/            entrada e ativação do portal do paciente (código pelo WhatsApp)
    assistencial/      tipos, API, rótulos e componentes do fluxo (histórico, ramo, escolha de horário)
    recepcao/          busca, cadastro e edição de pacientes, chegada e fila, agendamento
    triagem/           fila do clínico, avaliação, sinais vitais, prioridade, encaminhamentos, retificação
    especialista/      agenda da semana, plantão, consulta (iniciar, evolução, finalizar, falta), desfecho
    portal/            histórico do paciente, agendar e cancelar consultas
  shared/
    styles/token.css   cores, tipografia, espaços, raios e sombras
    api/               cliente HTTP e erros padronizados do backend
    sessao/            cookie de sessão (servidor), usuário atual e papéis (cliente)
    ui/                componentes do design system
    lib/, hooks/       datas, telefone, CPF, consultas com atualização automática
```

## Áreas por papel

| Papel | Início | Acesso |
| --- | --- | --- |
| Recepção e administração | `/` | Recepção, agendamentos, calendário, conversas e avisos |
| Clínico geral | `/triagem` | Fila e atendimento de triagem |
| Especialista | `/especialista` | Agenda, plantão e atendimentos |
| Paciente | `/portal` | Histórico, agendamento e cancelamento |

## Roteiro do fluxo assistencial

Usuários de demonstração do `db/demo/R__dados_demo.sql` (ver README da raiz). A fila, o plantão, as agendas e o portal da paciente Maria (`12345678909`) já começam com dados.

1. **Recepção** (`recepcao@liva.demo`): cadastre o paciente com consentimento; com "Paciente está na unidade" marcado, a chegada é registrada no mesmo passo.
2. **Triagem** (`clinico@liva.demo`): a triagem acontece presencialmente; a Liva só a registra. Em "Registrar triagem", lance avaliação, sinais vitais e prioridade. Encaminhe para uma especialidade (eletivo) e, em caso grave, para o plantão (imediato). Finalize.
3. **Plantão** (`plantao@liva.demo`): receba o encaminhamento imediato e conclua com o desfecho.
4. **Agendamento**: a recepção (ficha do paciente) ou o próprio paciente escolhe um horário livre. Dois cliques no mesmo horário não geram duas consultas.
5. **Portal** (`/portal/ativar`): CPF + data de nascimento → código pelo WhatsApp → senha. O paciente vê o histórico, agenda e cancela.
6. **Especialista** (`ortopedia@liva.demo`): no dia da consulta, inicie, registre a evolução e finalize (com ou sem retorno). Conclua o encaminhamento.
7. **Encerrar**: com todos os encaminhamentos concluídos ou cancelados, o clínico ou o especialista encerra o atendimento.

## Roteiro dos lembretes

1. **Agendar**: crie um atendimento com o seu WhatsApp. A confirmação chega na hora; se faltar 3 dias ou menos, o pedido "Você consegue ir?" também chega.
2. **Agendamentos**: o atendimento aparece com o status e a resposta do paciente ("Ainda não respondeu"). Filtre por período, status, nome ou telefone.
3. **Conversas**: responda 1, 2 ou 3, ou responda pelo próprio WhatsApp. O retorno é enviado ao paciente.
4. **Início / Avisos**: o status muda sozinho em até 10 s. Cancelamentos e remarcações aparecem em Avisos com a ação para a equipe.
5. **Detalhe do agendamento**: reenvie qualquer lembrete manualmente.
6. **Início**: use "Enviar lembretes do dia" para rodar a rotina diária sem esperar o agendador.
