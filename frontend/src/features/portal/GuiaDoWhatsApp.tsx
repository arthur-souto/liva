import { BellRing, CalendarCheck, CalendarClock, KeyRound, type LucideIcon } from "lucide-react";
import { Alerta } from "@/shared/ui/Alerta";
import estilos from "./GuiaDoWhatsApp.module.css";

type Fala = { de: "voce" | "liva"; texto: string };

const CONVERSA: Fala[] = [
  { de: "voce", texto: "oi" },
  {
    de: "liva",
    texto:
      "Olá, Maria! Sou a assistente da Liva. O que você quer fazer?\n1 - Agendar consulta\n2 - Ver minhas consultas\n3 - Remarcar consulta\n4 - Cancelar consulta\n0 - Sair",
  },
  { de: "voce", texto: "1" },
  {
    de: "liva",
    texto:
      "Horários livres para Cardiologia:\n1 - 18/11 (terça-feira), às 14:00\n2 - 19/11 (quarta-feira), às 09:30\nResponda com o número do horário.",
  },
  { de: "voce", texto: "1" },
  { de: "liva", texto: "Seu atendimento foi agendado para 18/11 (terça-feira), às 14:00. Vamos te lembrar alguns dias antes." },
];

const COMANDOS: { envie: string; para: string }[] = [
  { envie: "oi ou MENU", para: "Ver as opções" },
  { envie: "1", para: "Agendar consulta" },
  { envie: "2", para: "Ver minhas consultas" },
  { envie: "3", para: "Remarcar consulta" },
  { envie: "4", para: "Cancelar consulta" },
  { envie: "0 ou sair", para: "Encerrar a conversa" },
];

const AVISOS: { Icone: LucideIcon; quando: string; texto: string }[] = [
  { Icone: CalendarCheck, quando: "Ao agendar", texto: "Confirmação com o dia, o horário, a unidade e o endereço." },
  {
    Icone: BellRing,
    quando: "3 dias antes",
    texto: "Perguntamos se você vai. Responda 1 (sim, vou), 2 (não vou conseguir) ou 3 (quero outro horário).",
  },
  { Icone: BellRing, quando: "Na véspera", texto: "Lembrete com horário, endereço e preparo, se houver. Responda 2 se não puder ir." },
  {
    Icone: CalendarClock,
    quando: "Quando algo muda",
    texto: "Aviso de remarcação ou cancelamento, feito por você, pela sua pessoa de confiança ou pela recepção.",
  },
  {
    Icone: KeyRound,
    quando: "Para entrar no portal",
    texto: "Código de 6 números, que vale 10 minutos. Links para remarcar valem 30 minutos e funcionam uma vez.",
  },
];

export function GuiaDoWhatsApp() {
  return (
    <div className={estilos.guia}>
      <div className={estilos.colunas}>
        <figure className={estilos.celular} aria-label="Exemplo de conversa pelo WhatsApp">
          <figcaption className={estilos.contato}>
            <span className={estilos.avatar} aria-hidden="true">
              L
            </span>
            <span>
              <strong>Liva</strong>
              <span>exemplo de conversa</span>
            </span>
          </figcaption>
          <ol className={estilos.conversa}>
            {CONVERSA.map((fala, indice) => (
              <li key={indice} className={fala.de === "voce" ? estilos.enviada : estilos.recebida}>
                <span className={estilos.autor}>{fala.de === "voce" ? "Você" : "Liva"}</span>
                {fala.texto}
              </li>
            ))}
          </ol>
        </figure>

        <div className={estilos.lateral}>
          <h3 className={estilos.subtitulo}>O que enviar</h3>
          <dl className={estilos.comandos}>
            {COMANDOS.map(({ envie, para }) => (
              <div key={envie}>
                <dt>{envie}</dt>
                <dd>{para}</dd>
              </div>
            ))}
          </dl>
          <p className={estilos.nota}>Pode responder com o número ou por escrito, como “sim”, “remarcar” ou “cancelar”.</p>
        </div>
      </div>

      <div>
        <h3 className={estilos.subtitulo}>Mensagens que chegam sozinhas</h3>
        <ol className={estilos.linhaDoTempo}>
          {AVISOS.map(({ Icone, quando, texto }) => (
            <li key={quando}>
              <span className={estilos.marcador} aria-hidden="true">
                <Icone size={16} />
              </span>
              <div>
                <strong>{quando}</strong>
                <p>{texto}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>

      <Alerta tom="alerta" titulo="Cuidado com golpes">
        A Liva nunca pede senha, dados bancários ou pagamento pelo WhatsApp. Não passe o código de acesso para ninguém.
      </Alerta>
    </div>
  );
}
