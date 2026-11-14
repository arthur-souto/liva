"use client";

import { Pause, Play, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { detalharEntrada, pausarEntrada, retomarEntrada } from "./api";
import { ListaDeMovimentacoes } from "./ListaDeMovimentacoes";
import { PosicaoNoRamo } from "./PosicaoNoRamo";
import { STATUS_DA_OFERTA } from "./rotulos";
import estilos from "./PainelDaFila.module.css";

type Props = {
  entradaId: string;
  podeIntervir: boolean;
  aoFechar: () => void;
  aoAlterar: () => void;
};

export function GavetaDaEntrada({ entradaId, podeIntervir, aoFechar, aoAlterar }: Props) {
  const detalhe = useConsulta(() => detalharEntrada(entradaId), `entrada-${entradaId}`);
  const { executando, falha, executar } = useAcao();
  const [motivo, setMotivo] = useState("");
  const fechar = useRef<HTMLButtonElement>(null);
  const d = detalhe.dados;

  useEffect(() => {
    fechar.current?.focus();
    function aoTeclar(e: KeyboardEvent) {
      if (e.key === "Escape") aoFechar();
    }
    document.addEventListener("keydown", aoTeclar);
    return () => document.removeEventListener("keydown", aoTeclar);
  }, [aoFechar]);

  async function alterar(nome: string, acao: () => Promise<void>) {
    const feito = await executar(nome, async () => {
      await acao();
      return true;
    });
    if (!feito) return;
    setMotivo("");
    detalhe.recarregar();
    aoAlterar();
  }

  return (
    <div className={estilos.sobreposicao} onClick={aoFechar}>
      <aside
        className={estilos.gaveta}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-da-entrada"
        onClick={(e) => e.stopPropagation()}
      >
        <header className={estilos.topoGaveta}>
          <h2 id="titulo-da-entrada">{d?.nomePaciente ?? "Entrada na fila"}</h2>
          <button ref={fechar} type="button" className={estilos.fechar} onClick={aoFechar} aria-label="Fechar">
            <X size={20} aria-hidden="true" />
          </button>
        </header>
        {detalhe.erro ? <Alerta tom="perigo">{mensagemDeErro(detalhe.erro)}</Alerta> : null}
        {!d && !detalhe.erro && <Carregando linhas={4} />}
        {d && (
          <div className={estilos.corpoGaveta}>
            <PosicaoNoRamo fila={d.posicao} />

            {podeIntervir && d.posicao.status === "AGUARDANDO" && (
              <form
                className={estilos.intervencao}
                onSubmit={(e) => {
                  e.preventDefault();
                  alterar("pausar", () => pausarEntrada(entradaId, motivo.trim()));
                }}
              >
                <AreaDeTexto
                  rotulo="Pausar a fila desta pessoa"
                  dica="Ela não recebe ofertas enquanto estiver pausada. O motivo fica registrado."
                  value={motivo}
                  onChange={(e) => setMotivo(e.target.value)}
                  maxLength={500}
                  rows={2}
                  required
                />
                <Botao
                  type="submit"
                  variante="contorno"
                  icone={<Pause size={16} aria-hidden="true" />}
                  carregando={executando === "pausar"}
                  disabled={!motivo.trim()}
                >
                  Pausar
                </Botao>
              </form>
            )}
            {podeIntervir && d.posicao.status === "PAUSADA" && (
              <Botao
                icone={<Play size={16} aria-hidden="true" />}
                carregando={executando === "retomar"}
                onClick={() => alterar("retomar", () => retomarEntrada(entradaId))}
              >
                Retomar com a data de entrada original
              </Botao>
            )}
            {falha && <Alerta tom="perigo">{falha}</Alerta>}

            <section>
              <h3 className={estilos.subtitulo}>Ofertas</h3>
              {d.ofertas.length === 0 ? (
                <p className={estilos.vazio}>Nenhuma vaga oferecida ainda.</p>
              ) : (
                <ul className={estilos.listaSimples}>
                  {d.ofertas.map((o) => (
                    <li key={o.id}>
                      <div>
                        <strong>{formatarDataHora(o.inicio)}</strong>
                        <span>
                          Oferecida {formatarInstante(o.criadaEm)}
                          {o.respondidaEm && ` · respondida ${formatarInstante(o.respondidaEm)}`}
                          {o.canal && ` pelo ${o.canal === "PORTAL" ? "portal" : "WhatsApp"}`}
                        </span>
                        {o.motivo && <span>{o.motivo}</span>}
                      </div>
                      <Etiqueta tom={STATUS_DA_OFERTA[o.status].tom}>{STATUS_DA_OFERTA[o.status].texto}</Etiqueta>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section>
              <h3 className={estilos.subtitulo}>Histórico na fila</h3>
              <ListaDeMovimentacoes itens={d.movimentacoes} comPaciente={false} />
            </section>
          </div>
        )}
      </aside>
    </div>
  );
}
