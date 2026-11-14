import { CalendarPlus, CircleCheck, Eye } from "lucide-react";
import { ResultadoEnvios } from "@/features/agendamentos/componentes/ResultadoEnvios";
import { TIPOS_DE_ATENDIMENTO } from "@/features/agendamentos/rotulos";
import type { AgendamentoCriado } from "@/features/agendamentos/tipos";
import { formatarDataHora } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { Cartao } from "@/shared/ui/Cartao";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import estilos from "./ResultadoAgendamento.module.css";

type Props = {
  criado: AgendamentoCriado;
  aoRecomecar: () => void;
};

export function ResultadoAgendamento({ criado, aoRecomecar }: Props) {
  const { agendamento } = criado;
  return (
    <div className={estilos.pagina}>
      <Cartao variante="destaque" espacamento="amplo" className={estilos.hero}>
        <CircleCheck size={40} aria-hidden="true" className={estilos.icone} />
        <h1 className={estilos.titulo}>Agendamento criado</h1>
        <p className={estilos.quando}>
          {TIPOS_DE_ATENDIMENTO[agendamento.tipo]} · {formatarDataHora(agendamento.dataHora)}
        </p>
        <p className={estilos.unidade}>{agendamento.unidade}</p>
        <Etiqueta tom={criado.pacienteNovo ? "marca" : "info"}>{criado.pacienteNovo ? "Paciente novo" : "Paciente já cadastrado"}</Etiqueta>
      </Cartao>

      <Cartao className={estilos.envios}>
        <ResultadoEnvios titulo="Confirmação do agendamento" resultado={criado.confirmacao} />
        {criado.pedidoConfirmacao ? (
          <ResultadoEnvios titulo="Pedido de confirmação" resultado={criado.pedidoConfirmacao} />
        ) : (
          <Alerta tom="info" titulo="Pedido de confirmação agendado">
            A rotina diária pergunta &quot;Você consegue ir?&quot; perto da data do atendimento.
          </Alerta>
        )}
      </Cartao>

      <div className={estilos.acoes}>
        <BotaoLink href={`/agendamentos/${agendamento.id}`} variante="contorno" tamanho="grande" icone={<Eye size={18} aria-hidden="true" />}>
          Ver agendamento
        </BotaoLink>
        <Botao tamanho="grande" icone={<CalendarPlus size={18} aria-hidden="true" />} onClick={aoRecomecar}>
          Novo agendamento
        </Botao>
      </div>
    </div>
  );
}
