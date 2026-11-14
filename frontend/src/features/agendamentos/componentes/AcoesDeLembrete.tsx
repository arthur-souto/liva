"use client";

import { Send } from "lucide-react";
import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { dispararLembrete } from "../api";
import { LEMBRETES } from "../rotulos";
import type { ResultadoDisparo, TipoLembrete } from "../tipos";
import { ResultadoEnvios } from "./ResultadoEnvios";
import estilos from "./AcoesDeLembrete.module.css";

const ORDEM: TipoLembrete[] = ["PEDIDO_CONFIRMACAO", "LEMBRETE_VESPERA", "CONFIRMACAO_AGENDAMENTO"];

type Props = {
  agendamentoId: string;
  aoEnviar: () => void;
};

export function AcoesDeLembrete({ agendamentoId, aoEnviar }: Props) {
  const [confirmando, setConfirmando] = useState<TipoLembrete | null>(null);
  const [enviando, setEnviando] = useState<TipoLembrete | null>(null);
  const [resultado, setResultado] = useState<{ tipo: TipoLembrete; disparo: ResultadoDisparo } | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  async function enviar(tipo: TipoLembrete) {
    setEnviando(tipo);
    setErro(null);
    try {
      const disparo = await dispararLembrete(agendamentoId, tipo, true);
      setResultado({ tipo, disparo });
      aoEnviar();
    } catch (falha) {
      setErro(mensagemDeErro(falha));
    } finally {
      setEnviando(null);
      setConfirmando(null);
    }
  }

  return (
    <div className={estilos.acoes}>
      <ul className={estilos.lista}>
        {ORDEM.map((tipo) => (
          <li key={tipo} className={estilos.linha}>
            <div className={estilos.textos}>
              <p className={estilos.rotulo}>{LEMBRETES[tipo].rotulo}</p>
              <p className={estilos.descricao}>{LEMBRETES[tipo].descricao}</p>
            </div>
            {confirmando === tipo ? (
              <div className={estilos.confirmacao}>
                <Botao tamanho="pequeno" carregando={enviando === tipo} onClick={() => enviar(tipo)}>
                  Confirmar envio
                </Botao>
                <Botao tamanho="pequeno" variante="fantasma" disabled={enviando === tipo} onClick={() => setConfirmando(null)}>
                  Cancelar
                </Botao>
              </div>
            ) : (
              <Botao
                tamanho="pequeno"
                variante="secundario"
                icone={<Send size={16} aria-hidden="true" />}
                disabled={enviando !== null}
                onClick={() => setConfirmando(tipo)}
              >
                Enviar
              </Botao>
            )}
          </li>
        ))}
      </ul>
      {erro && <Alerta tom="perigo">{erro}</Alerta>}
      {resultado && <ResultadoEnvios titulo={LEMBRETES[resultado.tipo].rotulo} resultado={resultado.disparo} />}
    </div>
  );
}
