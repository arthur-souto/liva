# Pitch da Liva — 5 minutos

Desafio 1: redução de faltas em consultas e exames na Atenção Primária.

A ideia central do pitch: **a falta não é descuido, é atrito**. A pessoa esquece, não entende a mensagem, não consegue avisar que não vai ou não tem como chegar. A Liva tira esse atrito pelo WhatsApp que a pessoa já usa e mostra para a equipe quem precisa de uma ligação antes que o horário fique vazio.

---

## Antes de subir ao palco (15 minutos antes)

1. `docker compose up -d` e confira a instância do WhatsApp conectada (`state: open`).
2. Suba o backend e o front. Confira `curl localhost:8081/actuator/health` → `UP`.
3. Monte o cenário da apresentação, com o WhatsApp que vai estar na sua mão:

   ```bash
   scripts/preparar-apresentacao.sh +55DDDNUMERO
   ```

   O script recria a paciente **Rosa Oliveira** com datas relativas a agora e termina com `ok: o pedido de confirmação da Rosa é o mais recente deste WhatsApp`. Se aparecer `ATENÇÃO`, responda o pedido indicado ou rode o script de novo. Rode no máximo 10 minutos antes de começar.

4. Deixe abertas, nesta ordem:
   - **Aba 1:** `/faltas`, entrando como `admin@liva.demo`.
   - **Aba 2 (janela anônima):** `/especialista`, entrando como `ortopedia@liva.demo` (Dra. Ana Ribeiro). A consulta da Rosa de hoje aparece já passada.
   - **Celular:** WhatsApp aberto na conversa com a Liva, de preferência espelhado na tela.
5. Não mande mensagens para a Liva desde esse número antes da demonstração: uma conversa aberta com o assistente muda como a próxima resposta é lida.

A senha das contas é a senha de demonstração combinada com a equipe. A Rosa entra no portal pelo CPF `158.813.998-03` com a mesma senha, mas na demonstração ela entra pelo link que chega no WhatsApp, sem senha.

---

## Roteiro com tempos

### 0:00 – 0:50 · O problema

> "Toda semana, na UBS, uma parte das consultas especializadas fica vazia. A pessoa esperou meses na fila, a vaga existia, o profissional estava lá, e ninguém foi atendido.
>
> Quando a gente pergunta por que a pessoa faltou, a resposta quase nunca é 'não quis'. É 'esqueci', 'não tinha ônibus', 'não consegui sair do trabalho', 'tentei avisar e ninguém atendeu'.
>
> A falta é atrito: de memória, de comunicação, de informação e de acesso. E cada falta custa duas vezes — a vaga perdida e a pessoa que estava atrás na fila."

Se tiver um dado do seu município ou de um estudo, entre aqui com a fonte na tela. Não use número sem fonte.

### 0:50 – 1:20 · A solução em uma frase

> "A Liva é a camada de comparecimento da unidade. Ela lembra, pede confirmação, facilita cancelar e remarcar, devolve a vaga para a fila e mostra para a equipe quem precisa de uma ligação **antes** da falta — tudo pelo WhatsApp que a pessoa já usa, sem aplicativo novo."

### 1:20 – 2:10 · Cena 1: quem ligar primeiro (aba 1, `/faltas`)

1. Aponte a faixa roxa: "nas últimas 4 semanas, X consultas deixaram de ficar vazias" e a taxa de faltas caindo no gráfico.
2. Desça até **Ligar primeiro**. A **Rosa Oliveira** está no topo, com risco alto.
3. Leia os motivos do risco: faltou 2 vezes, não respondeu ao pedido de confirmação, consulta marcada há mais de 30 dias.

   > "A recepção não precisa ligar para todo mundo. Ela liga para quem tem chance real de faltar."

4. Toque em **Confirmou**. A Rosa passa para "Confirmado" e cai na lista.

   **Esse clique é obrigatório antes da cena 2:** ele encerra o pedido de confirmação pendente da Rosa, e é isso que permite a resposta da cena 2 ser lida como motivo.

### 2:10 – 3:10 · Cena 2: a falta vira aprendizado (aba 2 e celular)

1. Na aba do especialista, na consulta da Rosa de hoje, toque em **Registrar falta**.
2. No celular chega: "Sentimos sua falta…", com o link para remarcar e as 6 opções de motivo.

   > "Em vez de a falta virar só um número, a Liva pergunta por quê — e já oferece o caminho de volta."

3. Responda **2** (transporte). A Liva agradece e registra.
4. Volte à aba 1 e atualize: em **Por que as pessoas faltam**, transporte está no topo, com a ação sugerida para a gestão.

   > "Isso vira decisão: horários perto de casa, apoio de transporte, horários no início da manhã para quem trabalha."

### 3:10 – 4:00 · Cena 3: remarcar em um toque (celular)

1. Toque no link da mensagem. O portal abre já com a Rosa conectada, sem senha.
2. Escolha um horário e confirme.
3. Chega a confirmação no WhatsApp com **Como chegar** (abre o mapa) e **Salvar na agenda do celular**.

   > "Esquecimento e distância, resolvidos na própria mensagem. E quando alguém avisa que não vai, o horário volta na hora para a fila de espera, para outra pessoa."

### 4:00 – 4:40 · Por que funciona

- **Sem aplicativo novo:** tudo acontece no WhatsApp; o portal abre por link.
- **Fácil de avisar:** responder 2 libera a vaga na hora e a fila de espera oferece para o próximo.
- **A equipe age antes:** a lista "Ligar primeiro" concentra o esforço em quem tem risco alto.
- **A gestão aprende:** os motivos viram ações concretas.
- **Quem não usa WhatsApp não fica de fora:** quem não recebe a mensagem sobe na lista de ligação, e a recepção registra tudo pelo balcão.
- A Liva **registra**, não substitui a triagem nem o atendimento presencial.

### 4:40 – 5:00 · Fechamento

> "Queremos colocar a Liva em uma UBS por 60 dias e medir uma coisa só: quantas consultas deixaram de ficar vazias. O painel que vocês viram já mede isso desde o primeiro dia."

---

## Se algo der errado no palco

| Problema | O que fazer |
|---|---|
| WhatsApp não entrega | Abra **Conversas** no menu, escolha "Responder como paciente" e mostre a mesma conversa simulada |
| A resposta "2" cancelou uma consulta em vez de registrar o motivo | O pedido de confirmação ainda estava pendente: rode o script de novo e lembre de tocar em **Confirmou** na cena 1 |
| A Rosa não está no topo da lista | Toque em **Mostrar também … com risco baixo** e procure por ela; rode o script de novo depois |
| A consulta de hoje não aparece para o especialista | Rode o script de novo: ele cria a consulta 45 minutos antes do horário em que rodar |

---

## Perguntas prováveis

**E quem não tem WhatsApp?**
Quem não recebe a mensagem sobe na lista "Ligar primeiro". A recepção confirma por telefone e registra o resultado. O portal também funciona no navegador.

**E a LGPD?**
O cadastro registra o consentimento da pessoa. As mensagens levam só o necessário: data, hora, unidade e endereço. Toda ação da equipe fica auditada.

**Como vocês calculam o risco?**
É uma regra simples e explicável, não uma caixa-preta: faltas anteriores, pedido sem resposta, WhatsApp não entregue, consulta marcada há muito tempo. Cada pessoa da lista mostra por que está ali.

**De onde vem o número de consultas recuperadas?**
É uma estimativa: as faltas a menos, comparadas ao ritmo do início do período, somadas às vagas liberadas que foram ocupadas pela fila.

**O que vem depois?**
Lembrete em áudio para quem tem dificuldade de leitura, SMS quando o WhatsApp não chega e integração com o prontuário da unidade.
