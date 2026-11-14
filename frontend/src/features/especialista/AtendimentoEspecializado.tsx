"use client";

import { buscarPaciente, detalharTriagem } from "@/features/assistencial/api";
import { ResumoDoRamo } from "@/features/assistencial/componentes/ResumoDoRamo";
import { STATUS_DA_TRIAGEM } from "@/features/assistencial/rotulos";
import type { StatusEncaminhamento } from "@/features/assistencial/tipos";
import { EncerramentoDaTriagem } from "@/features/triagem/AcoesDaTriagem";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { idade } from "@/shared/lib/datas";
import { formatarTelefone } from "@/shared/lib/telefone";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import { AcoesDaConsulta } from "./AcoesDaConsulta";
import { AcoesDoEncaminhamento } from "./AcoesDoEncaminhamento";
import estilos from "./AtendimentoEspecializado.module.css";

const ENCERRADOS: StatusEncaminhamento[] = ["CONCLUIDO", "CANCELADO"];

export function AtendimentoEspecializado({ triagemId, consultaId }: { triagemId: string; consultaId: string | null }) {
  const ramo = useConsulta(() => detalharTriagem(triagemId), `ramo-${triagemId}`);
  const usuario = useConsulta(usuarioAtual, "usuario");
  const pacienteId = ramo.dados?.triagem.pacienteId ?? "";
  const paciente = useConsulta(() => (pacienteId ? buscarPaciente(pacienteId) : Promise.resolve(null)), `paciente-${pacienteId}`);

  if (ramo.erro) return <Alerta tom="perigo">{mensagemDeErro(ramo.erro)}</Alerta>;
  if (!ramo.dados) return <Carregando linhas={8} />;

  const { triagem, encaminhamentos, consultas } = ramo.dados;
  const eu = usuario.dados?.profissionalId;
  const minhas = consultas
    .filter((c) => c.profissionalId === eu)
    .sort((a, b) => Number(b.id === consultaId) - Number(a.id === consultaId) || b.inicio.localeCompare(a.inicio));
  const meusEncaminhamentos = encaminhamentos.filter(
    (e) => e.recebidoPor === eu || e.profissionalDestinoId === eu || consultas.some((c) => c.encaminhamentoId === e.id && c.profissionalId === eu),
  );
  const p = paciente.dados;

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo={p ? (p.nomeSocial ?? p.nomeCompleto) : "Atendimento"}
        subtitulo={p ? [p.dataNascimento && `${idade(p.dataNascimento)} anos`, formatarTelefone(p.telefone)].filter(Boolean).join(" · ") : undefined}
        acoes={<Etiqueta tom={STATUS_DA_TRIAGEM[triagem.status].tom}>{STATUS_DA_TRIAGEM[triagem.status].texto}</Etiqueta>}
      />

      <div className={estilos.colunas}>
        <div className={estilos.principal}>
          {minhas.length > 0 && (
            <Secao titulo="Minhas consultas">
              {minhas.map((c) => (
                <AcoesDaConsulta
                  key={`${c.id}-${c.versao}`}
                  consulta={c}
                  encaminhamento={encaminhamentos.find((e) => e.id === c.encaminhamentoId)}
                  destaque={c.id === consultaId}
                  aoAlterar={ramo.recarregar}
                />
              ))}
            </Secao>
          )}

          {meusEncaminhamentos.length > 0 && (
            <Secao titulo="Encaminhamentos" descricao="Conclua com o desfecho quando o acompanhamento terminar.">
              {meusEncaminhamentos.map((e) => (
                <AcoesDoEncaminhamento key={`${e.id}-${e.versao}`} encaminhamento={e} aoAlterar={ramo.recarregar} />
              ))}
            </Secao>
          )}

          {triagem.status === "FINALIZADA" && meusEncaminhamentos.length > 0 && (
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
          <Secao titulo="Triagem e histórico do ramo">
            <ResumoDoRamo ramo={ramo.dados} destaque={consultaId ?? undefined} />
          </Secao>
        </aside>
      </div>
    </div>
  );
}
