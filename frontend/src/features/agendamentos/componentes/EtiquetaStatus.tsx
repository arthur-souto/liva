import { Etiqueta } from "@/shared/ui/Etiqueta";
import { STATUS } from "../rotulos";
import type { StatusAgendamento } from "../tipos";

export function EtiquetaStatus({ status }: { status: StatusAgendamento }) {
  return <Etiqueta tom={STATUS[status].tom}>{STATUS[status].rotulo}</Etiqueta>;
}
