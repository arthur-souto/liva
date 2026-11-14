import {
  CalendarClock,
  CalendarPlus,
  CalendarX,
  CircleHelp,
  History,
  House,
  KeyRound,
  type LucideIcon,
  MessageCircle,
  ShieldCheck,
} from "lucide-react";
import type { ReactNode } from "react";
import { Alerta } from "@/shared/ui/Alerta";
import { Banner } from "@/shared/ui/Banner";
import { BotaoLink } from "@/shared/ui/Botao";
import { Cartao } from "@/shared/ui/Cartao";
import { GuiaDoWhatsApp } from "./GuiaDoWhatsApp";
import { PessoaComCelular, PessoasDeMaosDadas, PessoasNoPortal } from "@/shared/ui/Ilustracoes";
import estilos from "./GuiaDoPortal.module.css";

type Topico = {
  id: string;
  Icone: LucideIcon;
  titulo: string;
  resumo: string;
  passos: string[];
  dica?: string;
  link?: { href: string; texto: string };
  ilustracao?: ReactNode;
  extra?: ReactNode;
};

const TOPICOS: Topico[] = [
  {
    id: "acesso",
    Icone: KeyRound,
    titulo: "Entrar no portal",
    resumo: "O portal é liberado depois da sua primeira triagem na unidade.",
    passos: [
      "No primeiro acesso, toque em “Ative o portal do paciente” na tela de entrada.",
      "Informe o seu CPF ou Cartão SUS e a data de nascimento.",
      "Você recebe um código de 6 números no WhatsApp cadastrado na unidade.",
      "Digite o código e crie uma senha com pelo menos 8 caracteres, misturando letras e números.",
      "Nas próximas vezes, entre só com o CPF ou Cartão SUS e a senha.",
    ],
    dica: "Esqueceu a senha? Faça o mesmo caminho do primeiro acesso para criar uma nova. Depois de 5 senhas erradas, o acesso fica bloqueado por 15 minutos.",
  },
  {
    id: "inicio",
    Icone: House,
    titulo: "Tela inicial",
    resumo: "Um resumo do que precisa da sua atenção agora.",
    passos: [
      "Em “Escolha um horário” aparecem os encaminhamentos que a equipe liberou para você marcar.",
      "Em “Próxima consulta” você vê o dia, o horário, o profissional e o endereço.",
      "Use o menu para ir às suas consultas, ao histórico ou voltar a este guia.",
    ],
    link: { href: "/portal", texto: "Ir para o início" },
  },
  {
    id: "agendar",
    Icone: CalendarPlus,
    titulo: "Agendar uma consulta",
    resumo: "Quando um encaminhamento é liberado, você mesmo escolhe o dia e o horário.",
    passos: [
      "Na tela inicial, toque em “Escolher horário” no encaminhamento desejado.",
      "Escolha o dia na faixa da semana. Cada dia mostra quantos horários estão livres.",
      "Escolha o horário. Eles aparecem separados por profissional e por manhã, tarde e noite.",
      "Confira o resumo na barra de confirmação e toque em confirmar.",
      "Pronto: a confirmação chega no seu WhatsApp com o endereço da unidade.",
    ],
    dica: "Se o horário for ocupado por outra pessoa antes de você confirmar, é só escolher outro da lista atualizada.",
  },
  {
    id: "remarcar",
    Icone: CalendarClock,
    titulo: "Remarcar uma consulta",
    resumo: "Mudou algum compromisso? Troque o horário sem perder o encaminhamento.",
    passos: [
      "Abra “Minhas consultas” no menu.",
      "Na consulta que quer mudar, toque em “Remarcar”.",
      "Escolha o novo dia e horário, do mesmo jeito que no agendamento.",
      "O horário antigo é liberado e você recebe a nova data no WhatsApp.",
    ],
    dica: "Você pode remarcar até 2 vezes por mês. A tela de remarcação mostra quantas remarcações ainda restam.",
    link: { href: "/portal/consultas", texto: "Ver minhas consultas" },
  },
  {
    id: "cancelar",
    Icone: CalendarX,
    titulo: "Cancelar uma consulta",
    resumo: "Se não puder ir, cancele para que outra pessoa use o horário.",
    passos: [
      "Abra “Minhas consultas” no menu.",
      "Na consulta, toque em “Cancelar” e confirme.",
      "Você recebe o aviso de cancelamento no WhatsApp.",
      "Para marcar de novo, o encaminhamento volta a aparecer em “Escolha um horário”.",
    ],
  },
  {
    id: "historico",
    Icone: History,
    titulo: "Ver o seu histórico",
    resumo: "Tudo o que foi registrado na unidade, em ordem.",
    passos: [
      "Abra “Histórico” no menu.",
      "Cada triagem registrada aparece como um atendimento separado.",
      "Dentro dele você vê os encaminhamentos e as consultas que vieram daquela triagem.",
    ],
    link: { href: "/portal/historico", texto: "Abrir histórico" },
  },
  {
    id: "whatsapp",
    Icone: MessageCircle,
    titulo: "Usar pelo WhatsApp",
    resumo: "Agende, consulte, remarque e cancele por mensagem, sem precisar abrir o portal.",
    passos: [
      "Salve nos seus contatos o número que enviou a primeira mensagem da Liva.",
      "Use o WhatsApp do telefone cadastrado na unidade. Se o número mudou, avise a recepção.",
      "Envie qualquer mensagem, como “oi”, e escolha a opção pelo número.",
      "Para agendar, escolha um dos horários sugeridos. Para remarcar, você recebe um link que abre o portal já na consulta certa.",
      "A conversa fica aberta por 30 minutos. Depois disso, é só mandar “oi” de novo.",
    ],
    dica: "Se o mesmo número estiver cadastrado para mais de uma pessoa da família, a Liva pergunta primeiro sobre quem você quer falar.",
    ilustracao: <PessoaComCelular />,
    extra: <GuiaDoWhatsApp />,
  },
  {
    id: "confianca",
    Icone: ShieldCheck,
    titulo: "Pessoa de confiança",
    resumo: "Um familiar ou cuidador pode cuidar das suas consultas junto com você.",
    passos: [
      "Peça na recepção da unidade para registrar a pessoa, com nome, CPF e WhatsApp dela. É preciso a sua autorização ou a do responsável legal.",
      "Ela recebe as mesmas mensagens que você: confirmações, lembretes e avisos.",
      "Ela pode agendar, remarcar e cancelar pelo WhatsApp ou pelo portal, entrando em “Entrar como pessoa de confiança” com o CPF dela e o seu.",
      "Você pode pedir na recepção para retirar o acesso a qualquer momento.",
    ],
    ilustracao: <PessoasDeMaosDadas />,
  },
];

export function GuiaDoPortal() {
  return (
    <div className={estilos.pagina}>
      <Banner titulo="Como usar o portal" ilustracao={<PessoasNoPortal />}>
        Um passo a passo de tudo o que você consegue fazer pelo portal e pelo WhatsApp. Escolha um assunto abaixo.
      </Banner>

      <nav aria-label="Assuntos do guia">
        <ul className={estilos.indice}>
          {TOPICOS.map(({ id, Icone, titulo, resumo }) => (
            <li key={id}>
              <a href={`#${id}`} className={estilos.atalho}>
                <span className={estilos.icone} aria-hidden="true">
                  <Icone size={20} />
                </span>
                <span className={estilos.textoAtalho}>
                  <strong>{titulo}</strong>
                  <span>{resumo}</span>
                </span>
              </a>
            </li>
          ))}
        </ul>
      </nav>

      {TOPICOS.map(({ id, Icone, titulo, resumo, passos, dica, link, ilustracao, extra }) => (
        <Cartao key={id} como="section" espacamento="amplo" id={id} className={estilos.topico} aria-labelledby={`${id}-titulo`}>
          <div className={ilustracao ? estilos.comIlustracao : undefined}>
            <div className={estilos.conteudo}>
              <header className={estilos.cabecalhoTopico}>
                <span className={estilos.icone} aria-hidden="true">
                  <Icone size={20} />
                </span>
                <div>
                  <h2 id={`${id}-titulo`} className={estilos.titulo}>
                    {titulo}
                  </h2>
                  <p className={estilos.resumo}>{resumo}</p>
                </div>
              </header>
              <ol className={estilos.passos}>
                {passos.map((passo, indice) => (
                  <li key={passo}>
                    <span className={estilos.numero} aria-hidden="true">
                      {indice + 1}
                    </span>
                    <span>{passo}</span>
                  </li>
                ))}
              </ol>
              {dica && (
                <p className={estilos.dica}>
                  <CircleHelp size={16} aria-hidden="true" />
                  <span>{dica}</span>
                </p>
              )}
              {link && (
                <div>
                  <BotaoLink href={link.href} variante="contorno" tamanho="pequeno">
                    {link.texto}
                  </BotaoLink>
                </div>
              )}
            </div>
            {ilustracao && <div className={estilos.ilustracaoTopico}>{ilustracao}</div>}
          </div>
          {extra}
        </Cartao>
      ))}

      <Alerta tom="info" titulo="Ainda com dúvida?">
        A recepção da sua unidade de saúde ajuda com o cadastro, a troca do telefone e o acesso ao portal.
      </Alerta>
    </div>
  );
}
