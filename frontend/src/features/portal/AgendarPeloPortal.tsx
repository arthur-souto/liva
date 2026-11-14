"use client";

import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { EscolhaDeHorario } from "@/features/assistencial/componentes/EscolhaDeHorario";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Secao } from "@/shared/ui/Secao";

export function AgendarPeloPortal({ encaminhamentoId }: { encaminhamentoId: string }) {
  const router = useRouter();
  return (
    <>
      <CabecalhoPagina
        titulo="Agendar consulta"
        subtitulo="Escolha o melhor horário. Você recebe a confirmação pelo WhatsApp."
        acoes={
          <BotaoLink href="/portal" variante="fantasma" icone={<ArrowLeft size={18} aria-hidden="true" />}>
            Voltar
          </BotaoLink>
        }
      />
      <Secao titulo="Horários disponíveis">
        <EscolhaDeHorario encaminhamentoId={encaminhamentoId} aoAgendar={() => router.push("/portal")} />
      </Secao>
    </>
  );
}
