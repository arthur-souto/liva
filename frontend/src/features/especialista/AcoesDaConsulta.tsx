"use client";

import { CheckCircle2, NotebookPen, Play, UserX } from "lucide-react";
import { type FormEvent, useState } from "react";
import { finalizarConsulta, iniciarConsulta, registrarEvolucao, registrarFalta } from "@/features/assistencial/api";
import { NATUREZAS, STATUS_DA_CONSULTA } from "@/features/assistencial/rotulos";
import type { Consulta, Encaminhamento } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { classes } from "@/shared/lib/classes";
import { dataDe, formatarDataHora, hoje, horaAtual } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto } from "@/shared/ui/Campo";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import estilos from "./Acoes.module.css";

const AGUARDANDO = ["AGENDADO", "AGUARDANDO_RESPOSTA", "CONFIRMADO"];

type Props = {
  consulta: Consulta;
  encaminhamento: Encaminhamento | undefined;
  destaque: boolean;
  aoAlterar: () => void;
};

export function AcoesDaConsulta({ consulta, encaminhamento, destaque, aoAlterar }: Props) {
  const [evolucao, setEvolucao] = useState("");
  const [retorno, setRetorno] = useState(false);
  const acao = useAcao();
  const podeRetornar = encaminhamento?.modalidade === "ELETIVO";
  const agora = `${hoje()}T${horaAtual()}`;
  const podeIniciar = dataDe(consulta.inicio) <= hoje();
  const podeRegistrarFalta = consulta.inicio.slice(0, 16) <= agora;

  async function executar(nome: string, fn: () => Promise<unknown>, limpar = false) {
    if (await acao.executar(nome, fn)) {
      if (limpar) setEvolucao("");
      aoAlterar();
    }
  }

  function finalizar(evento: FormEvent) {
    evento.preventDefault();
    executar("finalizar", () => finalizarConsulta(consulta.id, evolucao.trim(), podeRetornar && retorno, consulta.versao), true);
  }

  return (
    <article className={classes(estilos.caixa, destaque && estilos.destaque)}>
      <header className={estilos.topo}>
        <strong>{formatarDataHora(consulta.inicio)}</strong>
        <Etiqueta tom={STATUS_DA_CONSULTA[consulta.status].tom}>{STATUS_DA_CONSULTA[consulta.status].texto}</Etiqueta>
        <span className={estilos.meta}>{NATUREZAS[consulta.natureza]}</span>
      </header>

      {AGUARDANDO.includes(consulta.status) && !podeIniciar && (
        <p className={estilos.meta}>A consulta pode ser iniciada no dia agendado.</p>
      )}

      {AGUARDANDO.includes(consulta.status) && podeIniciar && (
        <div className={estilos.acoes}>
          {podeRegistrarFalta && (
            <Botao
              variante="contorno"
              icone={<UserX size={18} aria-hidden="true" />}
              carregando={acao.executando === "falta"}
              onClick={() => executar("falta", () => registrarFalta(consulta.id, consulta.versao))}
            >
              Registrar falta
            </Botao>
          )}
          <Botao
            icone={<Play size={18} aria-hidden="true" />}
            carregando={acao.executando === "iniciar"}
            onClick={() => executar("iniciar", () => iniciarConsulta(consulta.id, consulta.versao))}
          >
            Iniciar consulta
          </Botao>
        </div>
      )}

      {consulta.status === "EM_ANDAMENTO" && (
        <form onSubmit={finalizar} className={estilos.formulario}>
          <AreaDeTexto rotulo="Evolução clínica" value={evolucao} onChange={(e) => setEvolucao(e.target.value)} rows={4} />
          {podeRetornar && (
            <label className={estilos.marcar}>
              <input type="checkbox" checked={retorno} onChange={(e) => setRetorno(e.target.checked)} />
              Autorizar retorno (o paciente poderá agendar uma nova consulta)
            </label>
          )}
          <div className={estilos.acoes}>
            <Botao
              variante="contorno"
              icone={<NotebookPen size={18} aria-hidden="true" />}
              carregando={acao.executando === "evolucao"}
              disabled={!evolucao.trim()}
              onClick={() => executar("evolucao", () => registrarEvolucao(consulta.id, evolucao.trim()), true)}
            >
              Salvar evolução
            </Botao>
            <Botao
              type="submit"
              icone={<CheckCircle2 size={18} aria-hidden="true" />}
              carregando={acao.executando === "finalizar"}
              disabled={!evolucao.trim()}
            >
              Finalizar consulta
            </Botao>
          </div>
        </form>
      )}

      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
    </article>
  );
}
