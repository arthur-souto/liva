import { CalendarClock, CircleCheck, CircleX, Clock, MessageCircleQuestion, Send, Stethoscope } from "lucide-react";
import { classes } from "@/shared/lib/classes";
import { RESPOSTA_DO_PACIENTE } from "../rotulos";
import type { StatusAgendamento } from "../tipos";
import estilos from "./RespostaDoPaciente.module.css";

const ICONES: Record<StatusAgendamento, typeof Clock> = {
  AGENDADO: Send,
  AGUARDANDO_RESPOSTA: MessageCircleQuestion,
  CONFIRMADO: CircleCheck,
  CANCELADO: CircleX,
  REAGENDADO: CalendarClock,
  COMPARECEU: CircleCheck,
  FALTOU: CircleX,
  EM_ANDAMENTO: Stethoscope,
};

export function RespostaDoPaciente({ status }: { status: StatusAgendamento }) {
  const Icone = ICONES[status];
  const { texto, tom } = RESPOSTA_DO_PACIENTE[status];
  return (
    <span className={classes(estilos.resposta, estilos[tom])}>
      <Icone size={14} aria-hidden="true" />
      {texto}
    </span>
  );
}
