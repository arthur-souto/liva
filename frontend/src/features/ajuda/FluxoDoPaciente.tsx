"use client";

import { CalendarCheck, CalendarPlus, ClipboardPen, HeartPulse, ListOrdered, type LucideIcon, Send, UserRoundPlus } from "lucide-react";
import { type CSSProperties, useState } from "react";
import { classes } from "@/shared/lib/classes";
import type { AreaDoGuia } from "./topicos";
import estilos from "./FluxoDoPaciente.module.css";

type Etapa = { id: string; Icone: LucideIcon; titulo: string; detalhe: string; area: AreaDoGuia; quem: string };

const ETAPAS: Etapa[] = [
  {
    id: "chegada",
    Icone: UserRoundPlus,
    titulo: "Chegada",
    detalhe: "A recepção encontra ou cadastra o paciente e registra a chegada. Ele entra na fila da triagem.",
    area: "recepcao",
    quem: "Recepção",
  },
  {
    id: "triagem",
    Icone: ClipboardPen,
    titulo: "Triagem",
    detalhe: "O profissional registra a avaliação feita na unidade e define a prioridade clínica.",
    area: "triagem",
    quem: "Clínico",
  },
  {
    id: "encaminhamento",
    Icone: Send,
    titulo: "Encaminhamento",
    detalhe: "Eletivo segue para agendamento e fila de espera. Imediato vai direto ao plantão do especialista.",
    area: "triagem",
    quem: "Clínico",
  },
  {
    id: "fila",
    Icone: ListOrdered,
    titulo: "Fila de espera",
    detalhe: "A ordem segue a prioridade e a data de entrada. Vagas liberadas são oferecidas a quem está na frente.",
    area: "filas",
    quem: "Automático",
  },
  {
    id: "agendamento",
    Icone: CalendarPlus,
    titulo: "Agendamento",
    detalhe: "O paciente marca pelo portal ou pelo WhatsApp, ou a recepção marca pelo balcão.",
    area: "recepcao",
    quem: "Paciente ou recepção",
  },
  {
    id: "confirmacao",
    Icone: CalendarCheck,
    titulo: "Confirmação",
    detalhe: "Dias antes, a Liva pergunta se o paciente vai. Quem não pode libera o horário para a fila.",
    area: "comunicacao",
    quem: "WhatsApp",
  },
  {
    id: "consulta",
    Icone: HeartPulse,
    titulo: "Consulta",
    detalhe: "O especialista registra a evolução e decide entre retorno e alta.",
    area: "especialista",
    quem: "Especialista",
  },
];

type Props = {
  area: AreaDoGuia;
  aoEscolher: (area: AreaDoGuia) => void;
};

export function FluxoDoPaciente({ area, aoEscolher }: Props) {
  const [foco, setFoco] = useState<Etapa | null>(null);
  const descrita = foco ?? ETAPAS.find((etapa) => etapa.area === area) ?? ETAPAS[0];

  return (
    <section className={estilos.fluxo} aria-labelledby="fluxo-titulo">
      <header className={estilos.cabecalho}>
        <h2 id="fluxo-titulo" className={estilos.titulo}>
          O caminho do paciente
        </h2>
        <p className={estilos.subtitulo}>Passe o mouse em cada etapa para entender. Toque para ver o passo a passo de quem cuida dela.</p>
      </header>

      <ol className={estilos.etapas} onMouseLeave={() => setFoco(null)}>
        {ETAPAS.map((etapa, indice) => {
          const daArea = etapa.area === area;
          return (
            <li key={etapa.id} className={estilos.item} style={{ "--atraso": `${indice * 90}ms` } as CSSProperties}>
              <button
                type="button"
                className={classes(estilos.etapa, daArea && estilos.daArea, foco?.id === etapa.id && estilos.emFoco)}
                onMouseEnter={() => setFoco(etapa)}
                onFocus={() => setFoco(etapa)}
                onBlur={() => setFoco(null)}
                onClick={() => aoEscolher(etapa.area)}
                aria-pressed={daArea}
                aria-describedby="fluxo-detalhe"
              >
                <span className={estilos.icone} aria-hidden="true">
                  <etapa.Icone size={20} />
                </span>
                <span className={estilos.nome}>{etapa.titulo}</span>
                <span className={estilos.quem}>{etapa.quem}</span>
              </button>
            </li>
          );
        })}
      </ol>

      <p id="fluxo-detalhe" className={estilos.detalhe} aria-live="polite">
        <strong>{descrita.titulo}.</strong> {descrita.detalhe}
      </p>
    </section>
  );
}
