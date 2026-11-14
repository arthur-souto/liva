"use client";

import { DoorOpen, Pencil } from "lucide-react";
import { buscarPaciente, cancelarConsulta, historicoDoPaciente, registrarChegada } from "@/features/assistencial/api";
import { HistoricoDeRamos } from "@/features/assistencial/componentes/HistoricoDeRamos";
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
import { mascararCartaoSus } from "./validacao";
import estilos from "./FichaDoPaciente.module.css";

export function FichaDoPaciente({ pacienteId }: { pacienteId: string }) {
  const paciente = useConsulta(() => buscarPaciente(pacienteId), `paciente-${pacienteId}`);
  const historico = useConsulta(() => historicoDoPaciente(pacienteId), `historico-${pacienteId}`);
  const acao = useAcao();

  async function chegada() {
    if (await acao.executar("chegada", () => registrarChegada(pacienteId))) historico.recarregar();
  }

  async function cancelar(consulta: Consulta) {
    if (await acao.executar(consulta.id, () => cancelarConsulta(consulta.id, consulta.versao))) historico.recarregar();
  }

  if (paciente.erro) return <Alerta tom="perigo">{mensagemDeErro(paciente.erro)}</Alerta>;
  if (!paciente.dados) return <Carregando linhas={6} />;
  const p = paciente.dados;
  const emAberto = historico.dados?.ramos.some((r) => r.triagem.status === "AGUARDANDO_ATENDIMENTO" || r.triagem.status === "EM_ANDAMENTO");

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

      <Secao titulo="Atendimentos" descricao="Cada triagem é um atendimento independente. A recepção vê só o andamento, sem dados clínicos.">
        {historico.erro ? <Alerta tom="perigo">{mensagemDeErro(historico.erro)}</Alerta> : null}
        {historico.carregando && !historico.dados && <Carregando />}
        {historico.dados && (
          <HistoricoDeRamos
            historico={historico.dados}
            linkParaAgendar={(encaminhamentoId) => `/recepcao/agendar/${encaminhamentoId}?paciente=${p.id}`}
            aoCancelar={cancelar}
            cancelando={acao.executando}
          />
        )}
      </Secao>
    </div>
  );
}
