import { Check } from "lucide-react";
import { classes } from "@/shared/lib/classes";
import { formatarInstante } from "@/shared/lib/datas";
import type { MensagemDoHistorico, StatusAgendamento } from "../tipos";
import { RespostaDoPaciente } from "./RespostaDoPaciente";
import estilos from "./AndamentoDaConfirmacao.module.css";

const SEM_PEDIDO: StatusAgendamento[] = ["AGENDADO"];
const SEM_RESPOSTA: StatusAgendamento[] = ["AGENDADO", "AGUARDANDO_RESPOSTA"];

type Props = {
  status: StatusAgendamento;
  ultimaResposta?: MensagemDoHistorico;
};

export function AndamentoDaConfirmacao({ status, ultimaResposta }: Props) {
  const etapas = [
    { titulo: "Agendamento criado", feita: true, descricao: "Confirmação enviada pelo WhatsApp." },
    {
      titulo: "Pedido de confirmação",
      feita: !SEM_PEDIDO.includes(status),
      descricao: SEM_PEDIDO.includes(status) ? "Sai pela rotina diária, 3 dias antes." : "Perguntamos \"Você consegue ir?\".",
    },
    { titulo: "Resposta do paciente", feita: !SEM_RESPOSTA.includes(status), descricao: null },
  ];
  const atual = etapas.findIndex((e) => !e.feita);

  return (
    <ol className={estilos.etapas}>
      {etapas.map((etapa, i) => (
        <li key={etapa.titulo} className={classes(estilos.etapa, etapa.feita && estilos.feita, i === atual && estilos.atual)}>
          <span className={estilos.marcador} aria-hidden="true">
            {etapa.feita ? <Check size={14} /> : i + 1}
          </span>
          <div className={estilos.texto}>
            <p className={estilos.titulo}>{etapa.titulo}</p>
            {etapa.descricao ? (
              <p className={estilos.descricao}>{etapa.descricao}</p>
            ) : (
              <>
                <RespostaDoPaciente status={status} />
                {ultimaResposta && (
                  <p className={estilos.citacao}>
                    &ldquo;{ultimaResposta.texto}&rdquo; · {formatarInstante(ultimaResposta.em)}
                  </p>
                )}
              </>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}
