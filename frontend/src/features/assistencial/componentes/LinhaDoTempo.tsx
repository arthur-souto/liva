import { Check, X } from "lucide-react";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { nomeDaEspecialidade } from "../rotulos";
import type { Consulta, StatusEncaminhamento } from "../tipos";
import estilos from "./LinhaDoTempo.module.css";

type Estado = "feito" | "atual" | "futuro" | "cancelado";
type Etapa = { titulo: string; detalhe: string | null; estado: Estado };

type Props = {
  chegadaEm: string;
  encaminhamento: { id: string; especialidade: string; status: StatusEncaminhamento; criadoEm: string };
  consultas: Consulta[];
};

const SEM_HORARIO = ["CANCELADO", "REAGENDADO"];

function etapas({ chegadaEm, encaminhamento, consultas }: Props): Etapa[] {
  const doEncaminhamento = consultas
    .filter((c) => c.encaminhamentoId === encaminhamento.id && !SEM_HORARIO.includes(c.status))
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  const realizadas = doEncaminhamento.filter((c) => c.status === "COMPARECEU");
  const ultimaRealizada = realizadas[realizadas.length - 1];
  const pendente = doEncaminhamento.find((c) => c.status !== "COMPARECEU" && c.status !== "FALTOU");
  const marcada = pendente ?? ultimaRealizada;
  const { status } = encaminhamento;
  const cancelado = status === "CANCELADO";
  const concluido = status === "CONCLUIDO";
  const aguardandoRetorno = realizadas.length > 0 && !concluido && !cancelado;

  const horario: Etapa = marcada
    ? { titulo: "Horário marcado", detalhe: formatarDataHora(marcada.inicio), estado: "feito" }
    : {
        titulo: "Horário marcado",
        detalhe:
          status === "PENDENTE" ? "A unidade vai liberar o agendamento" : status === "AGUARDANDO_AGENDAMENTO" ? "Escolha um horário" : null,
        estado: cancelado ? "futuro" : "atual",
      };
  const consulta: Etapa = ultimaRealizada
    ? { titulo: "Consulta realizada", detalhe: formatarDataHora(ultimaRealizada.inicio), estado: "feito" }
    : { titulo: "Consulta realizada", detalhe: null, estado: pendente && !cancelado ? "atual" : "futuro" };
  const fim: Etapa = cancelado
    ? { titulo: "Encaminhamento cancelado", detalhe: null, estado: "cancelado" }
    : concluido
      ? { titulo: "Alta", detalhe: "Acompanhamento concluído", estado: "feito" }
      : {
          titulo: aguardandoRetorno ? "Acompanhamento" : "Alta",
          detalhe: aguardandoRetorno
            ? status === "AGUARDANDO_AGENDAMENTO"
              ? "Retorno liberado: escolha um horário"
              : "Em acompanhamento com o especialista"
            : null,
          estado: aguardandoRetorno ? "atual" : "futuro",
        };

  return [
    { titulo: "Triagem", detalhe: formatarInstante(chegadaEm), estado: "feito" },
    {
      titulo: `Encaminhado para ${nomeDaEspecialidade(encaminhamento.especialidade)}`,
      detalhe: formatarInstante(encaminhamento.criadoEm),
      estado: "feito",
    },
    horario,
    consulta,
    fim,
  ];
}

export function LinhaDoTempo(props: Props) {
  const lista = etapas(props);
  return (
    <ol
      className={estilos.linha}
      aria-label={`Andamento do encaminhamento para ${nomeDaEspecialidade(props.encaminhamento.especialidade)}`}
    >
      {lista.map((e) => (
        <li key={e.titulo} className={estilos.etapa} data-estado={e.estado} aria-current={e.estado === "atual" ? "step" : undefined}>
          <span className={estilos.marcador} aria-hidden="true">
            {e.estado === "feito" && <Check size={14} strokeWidth={3} />}
            {e.estado === "cancelado" && <X size={14} strokeWidth={3} />}
          </span>
          <span className={estilos.textos}>
            <strong>{e.titulo}</strong>
            {e.detalhe && <span>{e.detalhe}</span>}
            {e.estado === "atual" && <span className={estilos.agora}>Etapa atual</span>}
          </span>
        </li>
      ))}
    </ol>
  );
}
