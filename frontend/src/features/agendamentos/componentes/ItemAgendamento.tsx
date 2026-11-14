import Link from "next/link";
import { horaDe } from "@/shared/lib/datas";
import { classes } from "@/shared/lib/classes";
import { formatarTelefone } from "@/shared/lib/telefone";
import { ACAO_DA_EQUIPE, TIPOS_DE_ATENDIMENTO } from "../rotulos";
import type { AgendamentoDaEquipe } from "../tipos";
import { EtiquetaStatus } from "./EtiquetaStatus";
import { RespostaDoPaciente } from "./RespostaDoPaciente";
import estilos from "./ItemAgendamento.module.css";

type Props = {
  agendamento: AgendamentoDaEquipe;
  destacado?: boolean;
};

export function ItemAgendamento({ agendamento: a, destacado = false }: Props) {
  const acao = ACAO_DA_EQUIPE[a.status];
  return (
    <Link
      href={`/agendamentos/${a.id}`}
      className={classes(estilos.item, acao && estilos.pedeAcao, destacado && estilos.destacado)}
    >
      <span className={estilos.horario}>
        <strong>{horaDe(a.dataHora)}</strong>
        <span>{TIPOS_DE_ATENDIMENTO[a.tipo]}</span>
      </span>
      <span className={estilos.info}>
        <span className={estilos.nome}>{a.nomePaciente ?? "Paciente sem cadastro"}</span>
        <span className={estilos.detalhe}>{a.unidade}</span>
        <span className={estilos.detalhe}>
          {formatarTelefone(a.telefone)}
          {a.nomeCuidador && ` · Cuidador: ${a.nomeCuidador}`}
        </span>
        <RespostaDoPaciente status={a.status} />
      </span>
      <span className={estilos.lado}>
        <EtiquetaStatus status={a.status} />
        {acao && <span className={estilos.acao}>{acao}</span>}
      </span>
    </Link>
  );
}
