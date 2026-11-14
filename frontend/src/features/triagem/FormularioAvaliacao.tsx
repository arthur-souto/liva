"use client";

import { CheckCircle2, Save } from "lucide-react";
import { type FormEvent, useState } from "react";
import { finalizarTriagem, registrarAvaliacao } from "@/features/assistencial/api";
import { ORDEM_DAS_PRIORIDADES, PRIORIDADES, prioridadeGrave } from "@/features/assistencial/rotulos";
import type { AvaliacaoClinica, Prioridade, SinaisVitais, Triagem } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto, Campo } from "@/shared/ui/Campo";
import { Chip } from "@/shared/ui/Chip";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./FormularioAvaliacao.module.css";

type Vitais = Record<keyof SinaisVitais, string>;

const CAMPOS_VITAIS: { campo: keyof SinaisVitais; rotulo: string; unidade: string; decimal?: boolean }[] = [
  { campo: "pressaoSistolica", rotulo: "PA sistólica", unidade: "mmHg" },
  { campo: "pressaoDiastolica", rotulo: "PA diastólica", unidade: "mmHg" },
  { campo: "frequenciaCardiaca", rotulo: "Freq. cardíaca", unidade: "bpm" },
  { campo: "frequenciaRespiratoria", rotulo: "Freq. respiratória", unidade: "irpm" },
  { campo: "temperatura", rotulo: "Temperatura", unidade: "°C", decimal: true },
  { campo: "saturacao", rotulo: "Saturação", unidade: "%" },
];

function vitaisDe(s: SinaisVitais | null): Vitais {
  const texto = (v: number | null | undefined) => (v === null || v === undefined ? "" : String(v).replace(".", ","));
  return {
    pressaoSistolica: texto(s?.pressaoSistolica),
    pressaoDiastolica: texto(s?.pressaoDiastolica),
    frequenciaCardiaca: texto(s?.frequenciaCardiaca),
    frequenciaRespiratoria: texto(s?.frequenciaRespiratoria),
    temperatura: texto(s?.temperatura),
    saturacao: texto(s?.saturacao),
  };
}

function numero(valor: string): number | null {
  const limpo = valor.trim().replace(",", ".");
  if (!limpo) return null;
  const n = Number(limpo);
  return Number.isFinite(n) ? n : null;
}

function texto(valor: string): string | null {
  return valor.trim() ? valor.trim() : null;
}

type Props = {
  triagem: Triagem;
  podeFinalizar: boolean;
  aoAlterar: () => void;
};

export function FormularioAvaliacao({ triagem, podeFinalizar, aoAlterar }: Props) {
  const [avaliacao, setAvaliacao] = useState<Record<keyof AvaliacaoClinica, string>>({
    queixaPrincipal: triagem.avaliacao?.queixaPrincipal ?? "",
    sintomas: triagem.avaliacao?.sintomas ?? "",
    historicoClinico: triagem.avaliacao?.historicoClinico ?? "",
    antecedentes: triagem.avaliacao?.antecedentes ?? "",
    observacoes: triagem.avaliacao?.observacoes ?? "",
  });
  const [vitais, setVitais] = useState<Vitais>(() => vitaisDe(triagem.sinaisVitais));
  const [prioridade, setPrioridade] = useState<Prioridade | null>(triagem.prioridade);
  const [justificativa, setJustificativa] = useState(triagem.justificativaPrioridade ?? "");
  const [aviso, setAviso] = useState<string | null>(null);
  const acao = useAcao();

  function alterarAvaliacao(campo: keyof AvaliacaoClinica, valor: string) {
    setAvaliacao((a) => ({ ...a, [campo]: valor }));
    setAviso(null);
  }

  function validar(finalizando: boolean): string | null {
    if (finalizando && !avaliacao.queixaPrincipal.trim()) return "Informe a queixa principal para finalizar.";
    if (finalizando && !prioridade) return "Classifique a prioridade para finalizar.";
    if (prioridadeGrave(prioridade) && !justificativa.trim()) return "Justifique a prioridade grave.";
    return null;
  }

  async function salvar(finalizando: boolean) {
    const problema = validar(finalizando);
    if (problema) {
      setAviso(problema);
      return;
    }
    const corpo: AvaliacaoClinica = {
      queixaPrincipal: texto(avaliacao.queixaPrincipal),
      sintomas: texto(avaliacao.sintomas),
      historicoClinico: texto(avaliacao.historicoClinico),
      antecedentes: texto(avaliacao.antecedentes),
      observacoes: texto(avaliacao.observacoes),
    };
    const sinais = Object.fromEntries(CAMPOS_VITAIS.map(({ campo }) => [campo, numero(vitais[campo])])) as SinaisVitais;
    const salva = await acao.executar(finalizando ? "finalizar" : "salvar", async () => {
      const atualizada = await registrarAvaliacao(triagem.id, corpo, sinais, prioridade, texto(justificativa), triagem.versao);
      return finalizando ? finalizarTriagem(triagem.id, atualizada.versao) : atualizada;
    });
    if (salva && !finalizando) setAviso("Avaliação salva.");
    aoAlterar();
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    salvar(false);
  }

  return (
    <form onSubmit={enviar} className={estilos.formulario} noValidate>
      <Secao titulo="Avaliação clínica">
        <AreaDeTexto rotulo="Queixa principal" value={avaliacao.queixaPrincipal} onChange={(e) => alterarAvaliacao("queixaPrincipal", e.target.value)} />
        <div className={estilos.grade}>
          <AreaDeTexto rotulo="Sintomas" opcional value={avaliacao.sintomas} onChange={(e) => alterarAvaliacao("sintomas", e.target.value)} />
          <AreaDeTexto rotulo="Histórico clínico" opcional value={avaliacao.historicoClinico} onChange={(e) => alterarAvaliacao("historicoClinico", e.target.value)} />
          <AreaDeTexto rotulo="Antecedentes" opcional value={avaliacao.antecedentes} onChange={(e) => alterarAvaliacao("antecedentes", e.target.value)} />
          <AreaDeTexto rotulo="Observações" opcional value={avaliacao.observacoes} onChange={(e) => alterarAvaliacao("observacoes", e.target.value)} />
        </div>
      </Secao>

      <Secao titulo="Sinais vitais" descricao="Preencha os que foram coletados.">
        <div className={estilos.vitais}>
          {CAMPOS_VITAIS.map(({ campo, rotulo, unidade, decimal }) => (
            <Campo
              key={campo}
              rotulo={`${rotulo} (${unidade})`}
              inputMode={decimal ? "decimal" : "numeric"}
              value={vitais[campo]}
              onChange={(e) => setVitais((v) => ({ ...v, [campo]: e.target.value.replace(/[^\d,.]/g, "") }))}
              opcional
            />
          ))}
        </div>
      </Secao>

      <Secao titulo="Prioridade">
        <div className={estilos.prioridades} role="group" aria-label="Prioridade">
          {ORDEM_DAS_PRIORIDADES.map((p) => (
            <Chip key={p} selecionado={prioridade === p} variante={prioridadeGrave(p) ? "realce" : "marca"} onClick={() => setPrioridade(p)}>
              {PRIORIDADES[p].texto}
            </Chip>
          ))}
        </div>
        {prioridadeGrave(prioridade) && (
          <AreaDeTexto
            rotulo="Justificativa da prioridade"
            value={justificativa}
            onChange={(e) => setJustificativa(e.target.value)}
            dica="Obrigatória para emergência e muito urgente."
          />
        )}
      </Secao>

      {aviso && <Alerta tom={aviso === "Avaliação salva." ? "sucesso" : "alerta"}>{aviso}</Alerta>}
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

      <div className={estilos.acoes}>
        <Botao type="submit" variante="contorno" icone={<Save size={18} aria-hidden="true" />} carregando={acao.executando === "salvar"}>
          Salvar
        </Botao>
        <Botao
          icone={<CheckCircle2 size={18} aria-hidden="true" />}
          carregando={acao.executando === "finalizar"}
          disabled={!podeFinalizar}
          onClick={() => salvar(true)}
        >
          Salvar e finalizar registro
        </Botao>
      </div>
    </form>
  );
}
