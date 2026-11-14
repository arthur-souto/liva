# Liva · Front-end

Painel da equipe da unidade de saúde para agendar atendimentos, acompanhar as respostas dos pacientes no WhatsApp e agir sobre cancelamentos e pedidos de remarcação.

Next.js 16 (App Router) + React 19 + TypeScript, organizado por feature, com o design system centralizado em `src/shared/styles/token.css`.

## Como rodar

1. Suba o backend (porta 8081) a partir da raiz do repositório.
2. Configure o front:

   ```bash
   cp .env.example .env.local
   ```

   Preencha `LIVA_API_KEY` com o mesmo valor de `API_KEY` do `.env` do backend.
3. Instale e rode:

   ```bash
   npm install
   npm run dev
   ```

4. Abra http://localhost:3000.

| Script | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento na porta 3000 |
| `npm run typecheck` | Checagem de tipos |

## Por que existe um proxy

O navegador nunca vê a chave da API. Toda chamada do front vai para `/api/liva/*`, um Route Handler do Next que roda no servidor, adiciona o cabeçalho `X-API-Key` a partir de `LIVA_API_KEY` e repassa para `LIVA_API_URL`.

- `LIVA_API_KEY` não tem o prefixo `NEXT_PUBLIC_`, então nunca entra no bundle do navegador.
- O webhook da Evolution (`/api/webhooks/**`) não é exposto pelo proxy.
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
  shared/
    styles/token.css   cores, tipografia, espaços, raios e sombras
    api/               cliente HTTP e erros padronizados do backend
    ui/                componentes do design system
    lib/, hooks/       datas, telefone, CPF, consultas com atualização automática
```

## Roteiro da demonstração

1. **Agendar**: crie um atendimento com o seu WhatsApp. A confirmação chega na hora; se faltar 3 dias ou menos, o pedido "Você consegue ir?" também chega.
2. **Agendamentos**: o atendimento aparece com o status e a resposta do paciente ("Ainda não respondeu"). Filtre por período, status, nome ou telefone.
3. **Conversas**: responda 1, 2 ou 3, ou responda pelo próprio WhatsApp. O retorno é enviado ao paciente.
4. **Início / Avisos**: o status muda sozinho em até 10 s. Cancelamentos e remarcações aparecem em Avisos com a ação para a equipe.
5. **Detalhe do agendamento**: reenvie qualquer lembrete manualmente.
6. **Início**: use "Enviar lembretes do dia" para rodar a rotina diária sem esperar o agendador.
