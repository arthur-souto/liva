"use client";

import { useRouter } from "next/navigation";
import { EscolhaDeHorario } from "@/features/assistencial/componentes/EscolhaDeHorario";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Secao } from "@/shared/ui/Secao";

export function AgendarPelaRecepcao({ encaminhamentoId, pacienteId }: { encaminhamentoId: string; pacienteId: string | null }) {
  const router = useRouter();
  return (
    <>
      <CabecalhoPagina titulo="Agendar consulta" subtitulo="Escolha um horário livre do especialista. O paciente recebe a confirmação pelo WhatsApp." />
      <Secao titulo="Horários disponíveis">
        <EscolhaDeHorario
          encaminhamentoId={encaminhamentoId}
          aoAgendar={(consulta) => router.push(`/recepcao/pacientes/${pacienteId ?? consulta.pacienteId}`)}
        />
      </Secao>
    </>
  );
}
