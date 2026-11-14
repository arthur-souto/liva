"use client";

import { Stethoscope } from "lucide-react";
import { buscarPaciente, detalharTriagem, historicoDoPaciente, iniciarTriagem } from "@/features/assistencial/api";
import { HistoricoDeRamos } from "@/features/assistencial/componentes/HistoricoDeRamos";
import { ResumoDoRamo } from "@/features/assistencial/componentes/ResumoDoRamo";
import { MODALIDADES, PRIORIDADES, STATUS_DA_TRIAGEM, STATUS_DO_ENCAMINHAMENTO, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import type { Historico, StatusEncaminhamento } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { formatarData, formatarInstante, idade } from "@/shared/lib/datas";
import { formatarTelefone } from "@/shared/lib/telefone";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import { CancelamentoDaTriagem, EncerramentoDaTriagem, RetificacaoDaTriagem } from "./AcoesDaTriagem";
import { FormularioAvaliacao } from "./FormularioAvaliacao";
import { FormularioEncaminhamento } from "./FormularioEncaminhamento";
import estilos from "./AtendimentoDeTriagem.module.css";

const ENCERRADOS: StatusEncaminhamento[] = ["CONCLUIDO", "CANCELADO"];

function anteriores(historico: Historico | null | undefined, triagemId: string): Historico | null {
  if (!historico) return null;
  return { ...historico, ramos: historico.ramos.filter((r) => r.triagem.id !== triagemId) };
}

export function AtendimentoDeTriagem({ triagemId }: { triagemId: string }) {
  const ramo = useConsulta(() => detalharTriagem(triagemId), `ramo-${triagemId}`);
  const usuario = useConsulta(usuarioAtual, "usuario");
  const pacienteId = ramo.dados?.triagem.pacienteId ?? "";
  const paciente = useConsulta(() => (pacienteId ? buscarPaciente(pacienteId) : Promise.resolve(null)), `paciente-${pacienteId}`);
  const historico = useConsulta(() => (pacienteId ? historicoDoPaciente(pacienteId) : Promise.resolve(null)), `historico-${pacienteId}`);
  const acao = useAcao();

  if (ramo.erro) return <Alerta tom="perigo">{mensagemDeErro(ramo.erro)}</Alerta>;
  if (!ramo.dados) return <Carregando linhas={8} />;

  const { triagem, encaminhamentos } = ramo.dados;
  const responsavel = triagem.profissionalId !== null && triagem.profissionalId === usuario.dados?.profissionalId;
  const emEdicao = triagem.status === "EM_ANDAMENTO" && responsavel;
  const concluida = triagem.status === "FINALIZADA" || triagem.status === "ENCERRADA";
  const p = paciente.dados;
  const passado = anteriores(historico.dados, triagemId);

  async function iniciar() {
    if (await acao.executar("iniciar", () => iniciarTriagem(triagemId))) ramo.recarregar();
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo={p ? (p.nomeSocial ?? p.nomeCompleto) : "Triagem"}
        subtitulo={p ? [p.dataNascimento && `${idade(p.dataNascimento)} anos`, `chegada ${formatarInstante(triagem.chegadaEm)}`].filter(Boolean).join(" · ") : undefined}
        acoes={<Etiqueta tom={STATUS_DA_TRIAGEM[triagem.status].tom}>{STATUS_DA_TRIAGEM[triagem.status].texto}</Etiqueta>}
      />
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

      <div className={estilos.colunas}>
        <div className={estilos.principal}>
          {triagem.status === "AGUARDANDO_ATENDIMENTO" && (
            <Secao titulo="Paciente aguardando" descricao="Ao iniciar, o registro desta triagem fica sob sua responsabilidade.">
              <div>
                <Botao icone={<Stethoscope size={18} aria-hidden="true" />} carregando={acao.executando === "iniciar"} onClick={iniciar}>
                  Iniciar registro
                </Botao>
              </div>
            </Secao>
          )}

          {triagem.status === "EM_ANDAMENTO" && !responsavel && (
            <Alerta tom="info">Esta triagem está sendo registrada por outro profissional. Você pode apenas consultar.</Alerta>
          )}

          {emEdicao && (
            <FormularioAvaliacao key={triagem.id} triagem={triagem} podeFinalizar aoAlterar={ramo.recarregar} />
          )}

          {emEdicao && (
            <Secao titulo="Encaminhamentos" descricao="Imediato vai direto ao plantão; eletivo é liberado para agendamento ao finalizar.">
              {encaminhamentos.length > 0 && (
                <ul className={estilos.encaminhamentos}>
                  {encaminhamentos.map((e) => (
                    <li key={e.id}>
                      <strong>{nomeDaEspecialidade(e.especialidade)}</strong>
                      <Etiqueta tom={STATUS_DO_ENCAMINHAMENTO[e.status].tom}>{STATUS_DO_ENCAMINHAMENTO[e.status].texto}</Etiqueta>
                      <Etiqueta tom={PRIORIDADES[e.prioridade].tom}>{PRIORIDADES[e.prioridade].texto}</Etiqueta>
                      <span className={estilos.meta}>{MODALIDADES[e.modalidade]}</span>
                    </li>
                  ))}
                </ul>
              )}
              <FormularioEncaminhamento triagemId={triagem.id} prioridadeDaTriagem={triagem.prioridade} aoEncaminhar={ramo.recarregar} />
            </Secao>
          )}

          {emEdicao && (
            <div className={estilos.cancelar}>
              <CancelamentoDaTriagem triagemId={triagem.id} aoCancelar={ramo.recarregar} />
            </div>
          )}

          {!emEdicao && triagem.status !== "AGUARDANDO_ATENDIMENTO" && (
            <Secao titulo="Registro da triagem">
              <ResumoDoRamo ramo={ramo.dados} />
            </Secao>
          )}

          {concluida && (
            <Secao titulo="Retificar">
              <RetificacaoDaTriagem triagemId={triagem.id} aoRetificar={ramo.recarregar} />
            </Secao>
          )}

          {triagem.status === "FINALIZADA" && responsavel && (
            <Secao titulo="Encerrar atendimento">
              <EncerramentoDaTriagem
                triagemId={triagem.id}
                liberado={encaminhamentos.every((e) => ENCERRADOS.includes(e.status))}
                aoEncerrar={ramo.recarregar}
              />
            </Secao>
          )}
        </div>

        <aside className={estilos.lateral}>
          <Secao titulo="Paciente">
            {p ? (
              <dl className={estilos.dados}>
                <div>
                  <dt>Nascimento</dt>
                  <dd>{p.dataNascimento ? formatarData(p.dataNascimento) : "—"}</dd>
                </div>
                <div>
                  <dt>Telefone</dt>
                  <dd>{formatarTelefone(p.telefone)}</dd>
                </div>
                {p.observacoes && (
                  <div>
                    <dt>Observações do cadastro</dt>
                    <dd>{p.observacoes}</dd>
                  </div>
                )}
              </dl>
            ) : (
              <Carregando linhas={2} />
            )}
          </Secao>
          {passado && passado.ramos.length > 0 && (
            <Secao titulo="Atendimentos anteriores">
              <HistoricoDeRamos ramos={passado.ramos} />
            </Secao>
          )}
        </aside>
      </div>
    </div>
  );
}
