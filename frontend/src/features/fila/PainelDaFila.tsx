"use client";

import { CalendarCheck, CalendarX2, Check, Clock, History, ListOrdered, PlayCircle, Send, Users, X } from "lucide-react";
import { useCallback, useState } from "react";
import { especialidades, especialistas } from "@/features/assistencial/api";
import { PRIORIDADES } from "@/features/assistencial/rotulos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useAtrasado } from "@/shared/hooks/useAtrasado";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { BarraDeFiltros } from "@/shared/ui/BarraDeFiltros";
import { Botao } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo, Selecao } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Paginacao } from "@/shared/ui/Paginacao";
import { Segmentado } from "@/shared/ui/Segmentado";
import { confirmarPresenca, painelDaFila, processarFila, registrarAceite, registrarRecusa } from "./api";
import { GavetaDaEntrada } from "./GavetaDaEntrada";
import { ListaDeMovimentacoes } from "./ListaDeMovimentacoes";
import { DECISAO_SEM_RESPOSTA, ORIGEM_DA_VAGA, STATUS_DA_ENTRADA, STATUS_DA_OFERTA, STATUS_DA_VAGA, ordinal, percentual } from "./rotulos";
import type { Indicadores, PainelDaFila as Painel, ResumoDoCiclo } from "./tipos";
import estilos from "./PainelDaFila.module.css";

type Aba = "fila" | "vagas" | "ofertas" | "confirmacao" | "movimentacoes";

const POR_PAGINA = 10;
const INTERVALO = 20000;
const SEM_FILTROS = { especialidade: "", profissional: "", unidade: "" };

function descreverCiclo(r: ResumoDoCiclo): string {
  const partes = [
    r.entradas > 0 && `${r.entradas} na fila`,
    r.vagas > 0 && `${r.vagas} ${r.vagas === 1 ? "vaga liberada" : "vagas liberadas"}`,
    r.ofertas > 0 && `${r.ofertas} ${r.ofertas === 1 ? "oferta enviada" : "ofertas enviadas"}`,
    r.expiradas > 0 && `${r.expiradas} ${r.expiradas === 1 ? "oferta expirada" : "ofertas expiradas"}`,
    r.avisos > 0 && `${r.avisos} ${r.avisos === 1 ? "aviso final" : "avisos finais"}`,
    r.prazos > 0 && `${r.prazos} ${r.prazos === 1 ? "prazo vencido tratado" : "prazos vencidos tratados"}`,
  ].filter(Boolean);
  return partes.length > 0 ? `Ciclo concluído: ${partes.join(", ")}.` : "Ciclo concluído. Nada pendente agora.";
}

function Indicador({ rotulo, valor, dica, tom }: { rotulo: string; valor: string | number; dica?: string; tom?: "alerta" | "sucesso" }) {
  return (
    <div className={classes(estilos.indicador, tom && estilos[tom])}>
      <span className={estilos.rotuloIndicador}>{rotulo}</span>
      <strong className={estilos.valorIndicador}>{valor}</strong>
      {dica && <span className={estilos.dicaIndicador}>{dica}</span>}
    </div>
  );
}

function PainelDeIndicadores({ i }: { i: Indicadores }) {
  return (
    <section className={estilos.indicadores} aria-label="Indicadores">
      <Indicador rotulo="Na fila" valor={i.aguardando} dica={i.pausadas > 0 ? `${i.pausadas} pausadas` : undefined} />
      <Indicador rotulo="Vagas abertas" valor={i.vagasAbertas} tom={i.vagasAbertas > 0 ? "alerta" : undefined} />
      <Indicador rotulo="Ofertas ativas" valor={i.ofertasAtivas} />
      <Indicador
        rotulo="Aguardando confirmação"
        valor={i.aguardandoConfirmacao}
        dica={i.semResposta > 0 ? `${i.semResposta} com prazo vencido` : undefined}
        tom={i.semResposta > 0 ? "alerta" : undefined}
      />
      <Indicador rotulo="Vagas reaproveitadas" valor={i.vagasReaproveitadas} dica="Últimos 30 dias" tom="sucesso" />
      <Indicador rotulo="Taxa de confirmação" valor={percentual(i.taxaDeConfirmacao)} dica="Últimos 30 dias" />
      <Indicador rotulo="Absenteísmo" valor={percentual(i.taxaDeAbsenteismo)} dica="Últimos 30 dias" />
      <Indicador rotulo="Recusadas / expiradas" valor={`${i.ofertasRecusadas} / ${i.ofertasExpiradas}`} dica="Últimos 30 dias" />
    </section>
  );
}

export function PainelDaFila() {
  const usuario = useConsulta(usuarioAtual, "usuario");
  const especialista = usuario.dados?.papel === "ESPECIALISTA";
  const [aba, setAba] = useState<Aba>("fila");
  const [filtros, setFiltros] = useState(SEM_FILTROS);
  const [pagina, setPagina] = useState(1);
  const [aberta, setAberta] = useState<string | null>(null);
  const [resumo, setResumo] = useState<string | null>(null);
  const unidade = useAtrasado(filtros.unidade.trim());
  const pedido = {
    especialidade: filtros.especialidade || undefined,
    profissional: filtros.profissional || undefined,
    unidade: unidade || undefined,
    pagina,
    tamanho: POR_PAGINA,
  };
  const painel = useConsulta(() => painelDaFila(pedido), `painel-fila|${JSON.stringify(pedido)}`, INTERVALO);
  const listaDeEspecialidades = useConsulta(especialidades, "especialidades");
  const profissionais = useConsulta(
    () => (filtros.especialidade ? especialistas(filtros.especialidade) : Promise.resolve([])),
    `especialistas-${filtros.especialidade}`,
  );
  const { executando, falha, executar } = useAcao();
  const p: Painel | null = painel.dados;
  const filtrando = Object.values(filtros).some(Boolean);
  const fecharGaveta = useCallback(() => setAberta(null), []);

  function mudar(campo: keyof typeof SEM_FILTROS, valor: string) {
    setFiltros((atuais) => ({ ...atuais, [campo]: valor, ...(campo === "especialidade" ? { profissional: "" } : {}) }));
    setPagina(1);
  }

  function limpar() {
    setFiltros(SEM_FILTROS);
    setPagina(1);
  }

  async function agir(nome: string, acao: () => Promise<unknown>, sucesso: string) {
    const feito = await executar(nome, async () => {
      await acao();
      return true;
    });
    if (!feito) return;
    setResumo(sucesso);
    painel.recarregar();
  }

  async function processar() {
    const r = await executar("processar", processarFila);
    if (!r) return;
    setResumo(descreverCiclo(r));
    painel.recarregar();
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Filas de espera"
        subtitulo={
          especialista
            ? "Fila da sua especialidade e as vagas da sua agenda reaproveitadas."
            : "Posição por encaminhamento, vagas liberadas, ofertas e confirmações de presença."
        }
        acoes={
          <div className={estilos.acoesTopo}>
            <AtualizadoHa atualizadoEm={painel.atualizadoEm} carregando={painel.carregando} aoAtualizar={painel.recarregar} />
            {p?.podeIntervir && (
              <Botao
                variante="contorno"
                tamanho="pequeno"
                icone={<PlayCircle size={16} aria-hidden="true" />}
                carregando={executando === "processar"}
                onClick={processar}
              >
                Processar agora
              </Botao>
            )}
          </div>
        }
      />

      {painel.erro ? <Alerta tom="perigo">{mensagemDeErro(painel.erro)}</Alerta> : null}
      {falha && <Alerta tom="perigo">{falha}</Alerta>}
      {resumo && !falha && <Alerta tom="sucesso">{resumo}</Alerta>}

      {usuario.dados && !especialista && (
        <BarraDeFiltros rotulo="Filtrar filas" filtrando={filtrando} aoLimpar={limpar}>
          <Selecao rotulo="Especialidade" value={filtros.especialidade} onChange={(e) => mudar("especialidade", e.target.value)}>
            <option value="">Todas</option>
            {(listaDeEspecialidades.dados ?? []).map((e) => (
              <option key={e.codigo} value={e.codigo}>
                {e.nome}
              </option>
            ))}
          </Selecao>
          <Selecao
            rotulo="Profissional"
            value={filtros.profissional}
            onChange={(e) => mudar("profissional", e.target.value)}
            disabled={!filtros.especialidade}
          >
            <option value="">Todos</option>
            {(profissionais.dados ?? []).map((prof) => (
              <option key={prof.id} value={prof.id}>
                {prof.nome}
              </option>
            ))}
          </Selecao>
          <Campo
            rotulo="Unidade"
            type="search"
            placeholder="Nome da unidade"
            value={filtros.unidade}
            onChange={(e) => mudar("unidade", e.target.value)}
          />
        </BarraDeFiltros>
      )}

      {!p && !painel.erro && <Carregando linhas={6} />}

      {p && (
        <>
          <PainelDeIndicadores i={p.indicadores} />

          <div className={estilos.abas}>
            <Segmentado
              rotulo="Visão do painel"
              opcoes={[
                { valor: "fila", rotulo: `Fila (${p.total})`, icone: <ListOrdered size={16} aria-hidden="true" /> },
                { valor: "vagas", rotulo: `Vagas (${p.vagas.length})`, icone: <CalendarX2 size={16} aria-hidden="true" /> },
                { valor: "ofertas", rotulo: `Ofertas (${p.ofertas.length})`, icone: <Send size={16} aria-hidden="true" /> },
                {
                  valor: "confirmacao",
                  rotulo: `Sem confirmação (${p.semConfirmacao.length})`,
                  icone: <Clock size={16} aria-hidden="true" />,
                },
                { valor: "movimentacoes", rotulo: "Movimentações", icone: <History size={16} aria-hidden="true" /> },
              ]}
              valor={aba}
              aoMudar={setAba}
            />
          </div>

          {aba === "fila" &&
            (p.itens.length === 0 ? (
              <EstadoVazio
                icone={<Users size={28} aria-hidden="true" />}
                titulo="Ninguém aguardando nesta fila"
                descricao="Encaminhamentos eletivos liberados entram aqui automaticamente."
              />
            ) : (
              <>
                <ul className={estilos.tabela}>
                  {p.itens.map(({ nomePaciente, posicao: f, ofertaAtiva }) => (
                    <li key={f.entradaId} className={estilos.linha}>
                      <span className={estilos.ordem}>{f.posicao ? ordinal(f.posicao) : "—"}</span>
                      <div className={estilos.principal}>
                        <strong>{nomePaciente}</strong>
                        <span>
                          {f.nomeEspecialidade} · Triagem #{String(f.triagemNumero).padStart(3, "0")}
                          {f.profissional && ` · ${f.profissional}`}
                        </span>
                        <span>
                          Na fila desde {formatarInstante(f.entrouEm)} ·{" "}
                          {f.consultaAtual ? `consulta em ${formatarDataHora(f.consultaAtual)}` : "sem horário marcado"}
                        </span>
                      </div>
                      <div className={estilos.etiquetas}>
                        <Etiqueta tom={PRIORIDADES[f.prioridade].tom}>{PRIORIDADES[f.prioridade].texto}</Etiqueta>
                        <Etiqueta tom={STATUS_DA_ENTRADA[f.status].tom}>{STATUS_DA_ENTRADA[f.status].texto}</Etiqueta>
                        {ofertaAtiva && <Etiqueta tom="destaque">Oferta até {formatarInstante(ofertaAtiva.expiraEm)}</Etiqueta>}
                      </div>
                      <Botao
                        variante="fantasma"
                        tamanho="pequeno"
                        onClick={() => setAberta(f.entradaId)}
                        aria-label={`Detalhes de ${nomePaciente}`}
                      >
                        Detalhes
                      </Botao>
                    </li>
                  ))}
                </ul>
                <Paginacao {...p} irPara={setPagina} carregando={painel.carregando} item={["pessoa", "pessoas"]} />
              </>
            ))}

          {aba === "vagas" &&
            (p.vagas.length === 0 ? (
              <EstadoVazio icone={<CalendarCheck size={28} aria-hidden="true" />} titulo="Nenhuma vaga liberada recentemente" />
            ) : (
              <ul className={estilos.tabela}>
                {p.vagas.map((v) => (
                  <li key={v.id} className={estilos.linha}>
                    <div className={estilos.principal}>
                      <strong>{formatarDataHora(v.inicio)}</strong>
                      <span>
                        {v.nomeEspecialidade}
                        {v.profissional && ` · ${v.profissional}`} · {v.unidade}
                      </span>
                      <span>
                        Liberada {formatarInstante(v.liberadaEm)} por {ORIGEM_DA_VAGA[v.origem].toLowerCase()} · {v.ofertas}{" "}
                        {v.ofertas === 1 ? "oferta" : "ofertas"}
                        {v.motivo && ` · ${v.motivo}`}
                      </span>
                    </div>
                    <Etiqueta tom={STATUS_DA_VAGA[v.status].tom}>{STATUS_DA_VAGA[v.status].texto}</Etiqueta>
                  </li>
                ))}
              </ul>
            ))}

          {aba === "ofertas" &&
            (p.ofertas.length === 0 ? (
              <EstadoVazio icone={<Send size={28} aria-hidden="true" />} titulo="Nenhuma oferta recente" />
            ) : (
              <ul className={estilos.tabela}>
                {p.ofertas.map((o) => {
                  const ativa = o.status === "PENDENTE" || o.status === "ENVIADA";
                  return (
                    <li key={o.id} className={estilos.linha}>
                      <div className={estilos.principal}>
                        <strong>{o.paciente}</strong>
                        <span>
                          {formatarDataHora(o.inicio)} · {o.nomeEspecialidade}
                          {o.profissional && ` · ${o.profissional}`}
                        </span>
                        <span>
                          {ativa
                            ? `Responder até ${formatarInstante(o.expiraEm)}`
                            : `${o.respondidaEm ? `Respondida ${formatarInstante(o.respondidaEm)}` : `Criada ${formatarInstante(o.criadaEm)}`}${
                                o.canal ? ` pelo ${o.canal === "PORTAL" ? "portal" : "WhatsApp"}` : ""
                              }`}
                          {o.motivo && ` · ${o.motivo}`}
                        </span>
                      </div>
                      <Etiqueta tom={STATUS_DA_OFERTA[o.status].tom}>{STATUS_DA_OFERTA[o.status].texto}</Etiqueta>
                      {ativa && p.podeIntervir && (
                        <div className={estilos.acoesLinha}>
                          <Botao
                            variante="contorno"
                            tamanho="pequeno"
                            icone={<X size={14} aria-hidden="true" />}
                            carregando={executando === `recusa-${o.id}`}
                            disabled={executando !== null}
                            onClick={() => agir(`recusa-${o.id}`, () => registrarRecusa(o.id), `Recusa de ${o.paciente} registrada.`)}
                          >
                            Registrar recusa
                          </Botao>
                          <Botao
                            tamanho="pequeno"
                            icone={<Check size={14} aria-hidden="true" />}
                            carregando={executando === `aceite-${o.id}`}
                            disabled={executando !== null}
                            onClick={() => agir(`aceite-${o.id}`, () => registrarAceite(o.id), `Aceite de ${o.paciente} registrado.`)}
                          >
                            Registrar aceite
                          </Botao>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            ))}

          {aba === "confirmacao" &&
            (p.semConfirmacao.length === 0 ? (
              <EstadoVazio icone={<CalendarCheck size={28} aria-hidden="true" />} titulo="Todas as consultas próximas estão confirmadas" />
            ) : (
              <ul className={estilos.tabela}>
                {p.semConfirmacao.map((c) => (
                  <li key={c.consultaId} className={estilos.linha}>
                    <div className={estilos.principal}>
                      <strong>{c.paciente}</strong>
                      <span>
                        {formatarDataHora(c.dataHora)} · {c.nomeEspecialidade}
                        {c.profissional && ` · ${c.profissional}`}
                      </span>
                      <span>
                        Confirmar até {formatarDataHora(c.confirmarAte)} ·{" "}
                        {!c.pedidoEnviado ? "pedido ainda não enviado" : c.pedidoEntregue ? "pedido entregue" : "entrega não confirmada"}
                      </span>
                    </div>
                    <div className={estilos.etiquetas}>
                      {c.decisao ? (
                        <Etiqueta tom={DECISAO_SEM_RESPOSTA[c.decisao].tom}>{DECISAO_SEM_RESPOSTA[c.decisao].texto}</Etiqueta>
                      ) : (
                        <Etiqueta tom="alerta">Aguardando resposta</Etiqueta>
                      )}
                    </div>
                    {p.podeIntervir && (
                      <Botao
                        variante="contorno"
                        tamanho="pequeno"
                        icone={<Check size={14} aria-hidden="true" />}
                        carregando={executando === `presenca-${c.consultaId}`}
                        disabled={executando !== null}
                        onClick={() =>
                          agir(`presenca-${c.consultaId}`, () => confirmarPresenca(c.consultaId), `Presença de ${c.paciente} confirmada.`)
                        }
                      >
                        Confirmar presença
                      </Botao>
                    )}
                  </li>
                ))}
              </ul>
            ))}

          {aba === "movimentacoes" &&
            (p.movimentacoes.length === 0 ? (
              <EstadoVazio icone={<History size={28} aria-hidden="true" />} titulo="Nenhuma movimentação ainda" />
            ) : (
              <ListaDeMovimentacoes itens={p.movimentacoes} />
            ))}
        </>
      )}

      {aberta && p && (
        <GavetaDaEntrada entradaId={aberta} podeIntervir={p.podeIntervir} aoFechar={fecharGaveta} aoAlterar={painel.recarregar} />
      )}
    </div>
  );
}
