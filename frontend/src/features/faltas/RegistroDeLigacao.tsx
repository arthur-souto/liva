"use client";

import { CalendarClock, Check, PhoneMissed } from "lucide-react";
import { useAcao } from "@/shared/hooks/useAcao";
import { formatarInstante } from "@/shared/lib/datas";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { registrarLigacao } from "./api";
import { LIGACOES, ORDEM_DAS_LIGACOES } from "./rotulos";
import type { ConsultaEmRisco, ResultadoDaLigacao } from "./tipos";
import estilos from "./PainelDeFaltas.module.css";

const ICONES: Record<ResultadoDaLigacao, typeof Check> = {
  CONFIRMOU: Check,
  VAI_REMARCAR: CalendarClock,
  NAO_ATENDEU: PhoneMissed,
};

export function RegistroDeLigacao({ consulta, aoRegistrar }: { consulta: ConsultaEmRisco; aoRegistrar: () => void }) {
  const acao = useAcao();
  const ultima = consulta.ultimaLigacao;

  async function registrar(resultado: ResultadoDaLigacao) {
    const feito = await acao.executar(resultado, async () => {
      await registrarLigacao(consulta.agendamentoId, resultado);
      return true;
    });
    if (feito) aoRegistrar();
  }

  return (
    <div className={estilos.ligacao}>
      {ultima && (
        <span className={estilos.ultimaLigacao}>
          <Etiqueta tom={LIGACOES[ultima.resultado].tom}>{LIGACOES[ultima.resultado].registrado}</Etiqueta>
          {formatarInstante(ultima.registradaEm)}
        </span>
      )}
      {consulta.status !== "CONFIRMADO" && (
        <div className={estilos.resultados} role="group" aria-label={`Resultado da ligação para ${consulta.nomePaciente}`}>
          <span className={estilos.perguntaLigacao}>Depois de ligar:</span>
          {ORDEM_DAS_LIGACOES.map((resultado) => {
            const Icone = ICONES[resultado];
            return (
              <button
                key={resultado}
                type="button"
                className={estilos.resultado}
                data-resultado={resultado}
                disabled={acao.executando !== null}
                aria-busy={acao.executando === resultado}
                onClick={() => registrar(resultado)}
              >
                <Icone size={14} aria-hidden="true" />
                {LIGACOES[resultado].rotulo}
              </button>
            );
          })}
        </div>
      )}
      {acao.falha && (
        <span className={estilos.falhaLigacao} role="alert">
          {acao.falha}
        </span>
      )}
    </div>
  );
}
