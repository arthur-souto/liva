"use client";

import { DoorOpen, Pencil } from "lucide-react";
import { useState } from "react";
import { atendimentosDoPaciente, buscarPaciente, cancelarConsulta, registrarChegada } from "@/features/assistencial/api";
import { AtendimentosPaginados } from "@/features/assistencial/componentes/AtendimentosPaginados";
import type { Consulta } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { mascararCpf } from "@/shared/lib/cpf";
import { formatarData } from "@/shared/lib/datas";
import { formatarTelefone } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import { PessoasDeConfianca } from "./PessoasDeConfianca";
import { mascararCartaoSus } from "./validacao";
import estilos from "./FichaDoPaciente.module.css";

export function FichaDoPaciente({ pacienteId, chegadaFalhou = false }: { pacienteId: string; chegadaFalhou?: boolean }) {
  const paciente = useConsulta(() => buscarPaciente(pacienteId), `paciente-${pacienteId}`);
  const [versao, setVersao] = useState(0);
  const ultimo = useConsulta(
    () => atendimentosDoPaciente(pacienteId, { pagina: 1, tamanho: 1 }),
    `ultimo-atendimento-${pacienteId}-${versao}`,
  );
  const acao = useAcao();

  async function chegada() {
    if (await acao.executar("chegada", () => registrarChegada(pacienteId))) setVersao((v) => v + 1);
  }

  async function cancelar(consulta: Consulta) {
    if (await acao.executar(consulta.id, () => cancelarConsulta(consulta.id, consulta.versao))) setVersao((v) => v + 1);
  }

  if (paciente.erro) return <Alerta tom="perigo">{mensagemDeErro(paciente.erro)}</Alerta>;
  if (!paciente.dados) return <Carregando linhas={6} />;
  const p = paciente.dados;
  const status = ultimo.dados?.itens[0]?.triagem.status;
  const emAberto = status === "AGUARDANDO_ATENDIMENTO" || status === "EM_ANDAMENTO";

  return (
    <div className={estilos.ficha}>
      <CabecalhoPagina
        titulo={p.nomeSocial ?? p.nomeCompleto}
        subtitulo={p.nomeSocial ? `Nome civil: ${p.nomeCompleto}` : undefined}
        acoes={
          <>
            <BotaoLink href={`/recepcao/pacientes/${p.id}/editar`} variante="contorno" icone={<Pencil size={16} aria-hidden="true" />}>
              Editar
            </BotaoLink>
            {emAberto ? (
              <Etiqueta tom="info">Na fila da triagem</Etiqueta>
            ) : (
              <Botao icone={<DoorOpen size={18} aria-hidden="true" />} carregando={acao.executando === "chegada"} onClick={chegada}>
                Registrar chegada
              </Botao>
            )}
          </>
        }
      />
      {chegadaFalhou && !emAberto && (
        <Alerta tom="alerta">O paciente foi cadastrado, mas não entrou na fila. Use "Registrar chegada" para incluí-lo na fila.</Alerta>
      )}
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

      <Secao titulo="Cadastro">
        <dl className={estilos.dados}>
          <div>
            <dt>CPF</dt>
            <dd>{p.cpf ? mascararCpf(p.cpf) : "—"}</dd>
          </div>
          <div>
            <dt>Cartão SUS</dt>
            <dd>{p.cartaoSus ? mascararCartaoSus(p.cartaoSus) : "—"}</dd>
          </div>
          <div>
            <dt>Nascimento</dt>
            <dd>{p.dataNascimento ? formatarData(p.dataNascimento) : "—"}</dd>
          </div>
          <div>
            <dt>Telefone</dt>
            <dd>{formatarTelefone(p.telefone)}</dd>
          </div>
          <div>
            <dt>Cuidador</dt>
            <dd>{p.nomeCuidador ? `${p.nomeCuidador} · ${formatarTelefone(p.telefoneCuidador)}` : "—"}</dd>
          </div>
          <div>
            <dt>WhatsApp</dt>
            <dd>{p.consentimento ? "Autorizado" : "Sem consentimento"}</dd>
          </div>
        </dl>
      </Secao>

      <PessoasDeConfianca pacienteId={p.id} cpfDoPaciente={p.cpf} />

      <Secao titulo="Atendimentos" descricao="Cada triagem é um atendimento independente. A recepção vê só o andamento, sem dados clínicos.">
        <AtendimentosPaginados
          chave={`atendimentos-${p.id}-${versao}`}
          carregar={(filtros) => atendimentosDoPaciente(p.id, filtros)}
          linkParaAgendar={(encaminhamentoId) => `/recepcao/agendar/${encaminhamentoId}?paciente=${p.id}`}
          aoCancelar={cancelar}
          cancelando={acao.executando}
        />
      </Secao>
    </div>
  );
}
