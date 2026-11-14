"use client";

import { Archive, FilePenLine, XCircle } from "lucide-react";
import { type FormEvent, useState } from "react";
import { cancelarTriagem, encerrarTriagem, retificarTriagem } from "@/features/assistencial/api";
import { useAcao } from "@/shared/hooks/useAcao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto } from "@/shared/ui/Campo";
import estilos from "./AcoesDaTriagem.module.css";

export function CancelamentoDaTriagem({ triagemId, aoCancelar }: { triagemId: string; aoCancelar: () => void }) {
  const [aberto, setAberto] = useState(false);
  const [motivo, setMotivo] = useState("");
  const acao = useAcao();

  async function confirmar(evento: FormEvent) {
    evento.preventDefault();
    if (await acao.executar("cancelar", () => cancelarTriagem(triagemId, motivo.trim()))) aoCancelar();
  }

  if (!aberto)
    return (
      <Botao variante="fantasma" icone={<XCircle size={18} aria-hidden="true" />} onClick={() => setAberto(true)}>
        Cancelar triagem
      </Botao>
    );

  return (
    <form onSubmit={confirmar} className={estilos.caixa}>
      <AreaDeTexto rotulo="Motivo do cancelamento" value={motivo} onChange={(e) => setMotivo(e.target.value)} dica="Ex.: paciente desistiu do atendimento." />
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      <div className={estilos.acoes}>
        <Botao variante="contorno" onClick={() => setAberto(false)}>
          Voltar
        </Botao>
        <Botao type="submit" variante="perigo" carregando={acao.executando === "cancelar"} disabled={!motivo.trim()}>
          Confirmar cancelamento
        </Botao>
      </div>
    </form>
  );
}

export function RetificacaoDaTriagem({ triagemId, aoRetificar }: { triagemId: string; aoRetificar: () => void }) {
  const [texto, setTexto] = useState("");
  const [justificativa, setJustificativa] = useState("");
  const acao = useAcao();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (await acao.executar("retificar", () => retificarTriagem(triagemId, texto.trim(), justificativa.trim()))) {
      setTexto("");
      setJustificativa("");
      aoRetificar();
    }
  }

  return (
    <form onSubmit={enviar} className={estilos.caixa}>
      <p className={estilos.dica}>A triagem finalizada não é alterada. A retificação fica registrada junto, com data e autor.</p>
      <AreaDeTexto rotulo="Informação corrigida ou complementar" value={texto} onChange={(e) => setTexto(e.target.value)} />
      <AreaDeTexto rotulo="Justificativa" value={justificativa} onChange={(e) => setJustificativa(e.target.value)} rows={2} />
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      <div className={estilos.acoes}>
        <Botao
          type="submit"
          variante="contorno"
          icone={<FilePenLine size={18} aria-hidden="true" />}
          carregando={acao.executando === "retificar"}
          disabled={!texto.trim() || !justificativa.trim()}
        >
          Registrar retificação
        </Botao>
      </div>
    </form>
  );
}

export function EncerramentoDaTriagem({ triagemId, liberado, aoEncerrar }: { triagemId: string; liberado: boolean; aoEncerrar: () => void }) {
  const acao = useAcao();

  async function encerrar() {
    if (await acao.executar("encerrar", () => encerrarTriagem(triagemId))) aoEncerrar();
  }

  return (
    <div className={estilos.caixa}>
      <p className={estilos.dica}>
        {liberado
          ? "Todos os encaminhamentos foram concluídos ou cancelados. O atendimento pode ser encerrado."
          : "Para encerrar, conclua ou cancele os encaminhamentos desta triagem."}
      </p>
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      <div className={estilos.acoes}>
        <Botao icone={<Archive size={18} aria-hidden="true" />} carregando={acao.executando === "encerrar"} disabled={!liberado} onClick={encerrar}>
          Encerrar atendimento
        </Botao>
      </div>
    </div>
  );
}
