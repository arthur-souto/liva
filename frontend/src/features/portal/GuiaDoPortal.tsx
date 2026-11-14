import {
  CalendarCheck,
  CalendarClock,
  CalendarPlus,
  CalendarX,
  History,
  House,
  KeyRound,
  ListOrdered,
  MessageCircle,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { Alerta } from "@/shared/ui/Alerta";
import { Banner } from "@/shared/ui/Banner";
import { Guia, type TopicoDoGuia } from "@/shared/ui/Guia";
import { GuiaDoWhatsApp } from "./GuiaDoWhatsApp";
import { PessoaComCelular, PessoasDeMaosDadas, PessoasNoPortal } from "@/shared/ui/Ilustracoes";

const TOPICOS: TopicoDoGuia[] = [
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
    id: "confirmar",
    Icone: CalendarCheck,
    titulo: "Confirmar presença",
    resumo: "Alguns dias antes da consulta, a Liva pergunta pelo WhatsApp se você consegue ir.",
    passos: [
      "Você recebe a mensagem “Você consegue ir?” com três opções.",
      "Responda 1 se vai, 2 se não vai conseguir ou 3 se quer outro horário. Também pode responder por escrito.",
      "Se responder 2, o horário é liberado e oferecido para outra pessoa da fila.",
      "Se responder 3, você recebe um link que abre o portal para escolher um novo horário. Isso conta como uma remarcação do mês.",
      "Na véspera chega um lembrete com o endereço e o preparo, se houver.",
    ],
    dica: "Ficar sem responder não cancela a sua consulta. Mas responder ajuda a unidade a não deixar horários vazios.",
  },
  {
    id: "fila",
    Icone: ListOrdered,
    titulo: "Acompanhar a fila",
    resumo: "Veja a sua posição, quantas pessoas estão na frente e uma previsão de quando sai a vaga.",
    passos: [
      "Abra “Minha fila” no menu. Cada encaminhamento aparece em um cartão separado.",
      "O círculo mostra quanto tempo falta, mais ou menos, pelo ritmo de vagas das últimas semanas.",
      "A faixa de pessoas mostra quem está na sua frente. Passe o mouse ou toque em cada pessoa para ver a posição.",
      "A linha do tempo mostra a triagem, a entrada na fila, hoje, a consulta marcada e a previsão da vaga.",
      "A ordem segue a prioridade definida na triagem e, depois, a data de entrada. Quem tem prioridade maior pode passar na frente.",
    ],
    dica: "A previsão é uma estimativa e pode mudar. Se você já tem consulta marcada, a fila serve para antecipar se surgir um horário mais cedo.",
    link: { href: "/portal/fila", texto: "Ver minha fila" },
  },
  {
    id: "vaga",
    Icone: Sparkles,
    titulo: "Quando aparece uma vaga",
    resumo: "Se alguém libera um horário, ele é oferecido para a próxima pessoa da fila.",
    passos: [
      "A oferta aparece no topo do portal e chega no seu WhatsApp, com o dia, o horário, o profissional e o local.",
      "O relógio no cartão mostra quanto tempo você tem para responder.",
      "Toque em “Aceitar vaga” para ficar com o horário. A consulta já fica confirmada.",
      "Se você já tem consulta marcada, o botão é “Trocar para este horário”: a consulta antiga passa para o novo horário e você continua com uma só.",
      "Se não puder, toque em “Não posso neste horário”. Você continua na fila na mesma posição.",
      "Pelo WhatsApp, responda ACEITO para ficar com a vaga ou NÃO POSSO.",
    ],
    dica: "Se o prazo acabar sem resposta, a vaga vai para a próxima pessoa e você continua na fila, sem perder o lugar.",
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
    <Guia
      rotulo="Assuntos do guia"
      topicos={TOPICOS}
      cabecalho={
        <Banner titulo="Como usar o portal" ilustracao={<PessoasNoPortal />}>
          Um passo a passo de tudo o que você consegue fazer pelo portal e pelo WhatsApp. Escolha um assunto abaixo.
        </Banner>
      }
      rodape={
        <Alerta tom="info" titulo="Ainda com dúvida?">
          A recepção da sua unidade de saúde ajuda com o cadastro, a troca do telefone e o acesso ao portal.
        </Alerta>
      }
    />
  );
}
