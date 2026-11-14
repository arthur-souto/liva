"use client";

import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { minhasRemarcacoes } from "@/features/assistencial/api";
import { EscolhaDeHorario } from "@/features/assistencial/componentes/EscolhaDeHorario";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./Portal.module.css";

type Props = { encaminhamentoId: string; remarcacao: { consultaId: string; versao: number } | null };

export function AgendarPeloPortal({ encaminhamentoId, remarcacao }: Props) {
  const router = useRouter();
  const voltar = remarcacao ? "/portal/consultas" : "/portal";
  const remarcacoes = useConsulta(
    () => (remarcacao ? minhasRemarcacoes() : Promise.resolve(null)),
    `remarcacoes-${remarcacao?.consultaId ?? ""}`,
  );
  const restantes = remarcacoes.dados?.restantes;
  const bloqueado = remarcacao !== null && restantes === 0;

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo={remarcacao ? "Remarcar consulta" : "Agendar consulta"}
        subtitulo="Escolha o melhor horário. Você recebe a confirmação pelo WhatsApp."
        acoes={
          <BotaoLink href={voltar} variante="fantasma" icone={<ArrowLeft size={18} aria-hidden="true" />}>
            Voltar
          </BotaoLink>
        }
      />
      {remarcacao && restantes !== undefined && !bloqueado && (
        <Alerta tom="info">
          {`Seu horário atual só é liberado quando você confirmar o novo. Você pode remarcar mais ${restantes} ${restantes === 1 ? "vez" : "vezes"} este mês.`}
        </Alerta>
      )}
      {bloqueado && (
        <Alerta tom="perigo">
          {`Você já remarcou ${remarcacoes.dados?.limite} vezes este mês, que é o limite. Para mudar de novo, procure a recepção da unidade.`}
        </Alerta>
      )}
      {remarcacao && remarcacoes.carregando && !remarcacoes.dados && <Carregando linhas={2} />}
      {(!remarcacao || Boolean(remarcacoes.erro) || (restantes !== undefined && !bloqueado)) && (
        <Secao titulo="Horários disponíveis">
          <EscolhaDeHorario
            encaminhamentoId={encaminhamentoId}
            remarcacao={remarcacao}
            aoAgendar={() => router.push("/portal/consultas")}
          />
        </Secao>
      )}
    </div>
  );
}
