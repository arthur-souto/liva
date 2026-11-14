"use client";

import { CheckCircle2, RotateCcw } from "lucide-react";
import { type FormEvent, useState } from "react";
import { autorizarRetorno, concluirEncaminhamento } from "@/features/assistencial/api";
import { MODALIDADES, PRIORIDADES, STATUS_DO_ENCAMINHAMENTO, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import type { Encaminhamento } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto } from "@/shared/ui/Campo";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import estilos from "./Acoes.module.css";

export function AcoesDoEncaminhamento({ encaminhamento, aoAlterar }: { encaminhamento: Encaminhamento; aoAlterar: () => void }) {
  const [desfecho, setDesfecho] = useState("");
  const acao = useAcao();
  const e = encaminhamento;
  const podeConcluir = e.status === "EM_ATENDIMENTO" || e.status === "AGUARDANDO_AGENDAMENTO";
  const podeRetornar = e.status === "EM_ATENDIMENTO" && e.modalidade === "ELETIVO";

  async function concluir(evento: FormEvent) {
    evento.preventDefault();
    if (await acao.executar("concluir", () => concluirEncaminhamento(e.id, desfecho.trim()))) aoAlterar();
  }

  async function retornar() {
    if (await acao.executar("retorno", () => autorizarRetorno(e.id))) aoAlterar();
  }

  return (
    <article className={estilos.caixa}>
      <header className={estilos.topo}>
        <strong>{nomeDaEspecialidade(e.especialidade)}</strong>
        <Etiqueta tom={STATUS_DO_ENCAMINHAMENTO[e.status].tom}>{STATUS_DO_ENCAMINHAMENTO[e.status].texto}</Etiqueta>
        <Etiqueta tom={PRIORIDADES[e.prioridade].tom}>{PRIORIDADES[e.prioridade].texto}</Etiqueta>
        <span className={estilos.meta}>{MODALIDADES[e.modalidade]}</span>
      </header>
      <p className={estilos.texto}>{e.justificativa}</p>
      {e.desfecho && <p className={estilos.texto}>Desfecho: {e.desfecho}</p>}

      {podeConcluir && (
        <form onSubmit={concluir} className={estilos.formulario}>
          <AreaDeTexto
            rotulo="Desfecho do acompanhamento"
            value={desfecho}
            onChange={(ev) => setDesfecho(ev.target.value)}
            rows={2}
            dica="Ex.: alta da especialidade, contrarreferência à atenção básica."
          />
          <div className={estilos.acoes}>
            {podeRetornar && (
              <Botao variante="contorno" icone={<RotateCcw size={18} aria-hidden="true" />} carregando={acao.executando === "retorno"} onClick={retornar}>
                Autorizar retorno
              </Botao>
            )}
            <Botao
              type="submit"
              icone={<CheckCircle2 size={18} aria-hidden="true" />}
              carregando={acao.executando === "concluir"}
              disabled={!desfecho.trim()}
            >
              Concluir encaminhamento
            </Botao>
          </div>
        </form>
      )}

      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
    </article>
  );
}
