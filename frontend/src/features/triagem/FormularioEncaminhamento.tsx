"use client";

import { Send } from "lucide-react";
import { type FormEvent, useState } from "react";
import { encaminhar, especialidades, especialistas } from "@/features/assistencial/api";
import { MODALIDADES, ORDEM_DAS_PRIORIDADES, PRIORIDADES, prioridadeGrave } from "@/features/assistencial/rotulos";
import type { Modalidade, Prioridade } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto, Selecao } from "@/shared/ui/Campo";
import { Segmentado } from "@/shared/ui/Segmentado";
import estilos from "./FormularioEncaminhamento.module.css";

const OPCOES_MODALIDADE = (Object.keys(MODALIDADES) as Modalidade[]).map((valor) => ({ valor, rotulo: MODALIDADES[valor] }));

type Props = {
  triagemId: string;
  prioridadeDaTriagem: Prioridade | null;
  aoEncaminhar: () => void;
};

export function FormularioEncaminhamento({ triagemId, prioridadeDaTriagem, aoEncaminhar }: Props) {
  const catalogo = useConsulta(especialidades, "especialidades");
  const [especialidade, setEspecialidade] = useState("");
  const [destino, setDestino] = useState("");
  const [modalidade, setModalidade] = useState<Modalidade>("ELETIVO");
  const [prioridade, setPrioridade] = useState<Prioridade>(prioridadeDaTriagem ?? "POUCO_URGENTE");
  const [justificativa, setJustificativa] = useState("");
  const [aviso, setAviso] = useState<string | null>(null);
  const medicos = useConsulta(() => (especialidade ? especialistas(especialidade) : Promise.resolve([])), `especialistas-${especialidade}`);
  const acao = useAcao();

  function escolherModalidade(valor: Modalidade) {
    setModalidade(valor);
    if (valor === "IMEDIATO") {
      const plantao = catalogo.dados?.find((e) => e.plantao);
      if (plantao) setEspecialidade(plantao.codigo);
      if (!prioridadeGrave(prioridade)) setPrioridade("EMERGENCIA");
    }
    setDestino("");
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setAviso(null);
    const problema = !especialidade
      ? "Escolha a especialidade."
      : !justificativa.trim()
        ? "Descreva o motivo do encaminhamento."
        : modalidade === "IMEDIATO" && !prioridadeGrave(prioridade)
          ? "Encaminhamento imediato exige prioridade grave."
          : null;
    if (problema) {
      setAviso(problema);
      return;
    }
    const criado = await acao.executar("encaminhar", () =>
      encaminhar(triagemId, { especialidade, profissionalDestinoId: destino || null, modalidade, prioridade, justificativa: justificativa.trim() }),
    );
    if (criado) {
      setJustificativa("");
      setDestino("");
      aoEncaminhar();
    }
  }

  return (
    <form onSubmit={enviar} className={estilos.formulario} noValidate>
      <Segmentado rotulo="Modalidade" opcoes={OPCOES_MODALIDADE} valor={modalidade} aoMudar={escolherModalidade} />
      <div className={estilos.grade}>
        <Selecao
          rotulo="Especialidade"
          value={especialidade}
          onChange={(e) => {
            setEspecialidade(e.target.value);
            setDestino("");
          }}
        >
          <option value="">Selecione</option>
          {(catalogo.dados ?? []).map((e) => (
            <option key={e.codigo} value={e.codigo}>
              {e.nome}
            </option>
          ))}
        </Selecao>
        <Selecao rotulo="Profissional" opcional value={destino} onChange={(e) => setDestino(e.target.value)} disabled={!especialidade}>
          <option value="">Qualquer profissional da especialidade</option>
          {(medicos.dados ?? []).map((m) => (
            <option key={m.id} value={m.id}>
              {m.nome}
            </option>
          ))}
        </Selecao>
        <Selecao rotulo="Prioridade" value={prioridade} onChange={(e) => setPrioridade(e.target.value as Prioridade)}>
          {ORDEM_DAS_PRIORIDADES.map((p) => (
            <option key={p} value={p}>
              {PRIORIDADES[p].texto}
            </option>
          ))}
        </Selecao>
      </div>
      <AreaDeTexto rotulo="Motivo do encaminhamento" value={justificativa} onChange={(e) => setJustificativa(e.target.value)} />
      {modalidade === "ELETIVO" && (
        <p className={estilos.dica}>O paciente poderá agendar depois que a triagem for finalizada.</p>
      )}
      {aviso && <Alerta tom="alerta">{aviso}</Alerta>}
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      <div className={estilos.acoes}>
        <Botao type="submit" variante={modalidade === "IMEDIATO" ? "perigo" : "primario"} icone={<Send size={18} aria-hidden="true" />} carregando={acao.executando === "encaminhar"}>
          {modalidade === "IMEDIATO" ? "Encaminhar ao plantão agora" : "Encaminhar"}
        </Botao>
      </div>
    </form>
  );
}
