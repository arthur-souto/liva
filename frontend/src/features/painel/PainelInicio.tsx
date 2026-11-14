"use client";

import { ArrowRight, CalendarCheck2, CalendarDays, CalendarPlus, Clock, Inbox, Send, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { executarRotinaDiaria, listarAgendamentos } from "@/features/agendamentos/api";
import { EtiquetaStatus } from "@/features/agendamentos/componentes/EtiquetaStatus";
import { ListaAgendamentos } from "@/features/agendamentos/componentes/ListaAgendamentos";
import { STATUS } from "@/features/agendamentos/rotulos";
import type { AgendamentoDaEquipe, StatusAgendamento } from "@/features/agendamentos/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { adicionarDias, formatarDataCurta, horaAtual, horaDe, hoje, montarDataHora, nomeDiaCurto } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { Cartao } from "@/shared/ui/Cartao";
import { Chip } from "@/shared/ui/Chip";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import estilos from "./PainelInicio.module.css";

type Periodo = "hoje" | "amanha" | "semana" | "personalizado";

const PERIODOS: { valor: Periodo; rotulo: string }[] = [
  { valor: "hoje", rotulo: "Hoje" },
  { valor: "amanha", rotulo: "Amanhã" },
  { valor: "semana", rotulo: "Próximos 7 dias" },
  { valor: "personalizado", rotulo: "Personalizado" },
];

const RESUMO: StatusAgendamento[] = ["AGUARDANDO_RESPOSTA", "CONFIRMADO", "REAGENDADO", "CANCELADO"];
const FORA_DO_PROXIMO: StatusAgendamento[] = ["CANCELADO", "REAGENDADO", "FALTOU", "COMPARECEU"];
const INTERVALO = 10000;

function intervalo(periodo: Periodo, personalizado: { inicio: string; fim: string }) {
  const referencia = hoje();
  if (periodo === "amanha") return { inicio: adicionarDias(referencia, 1), fim: adicionarDias(referencia, 1) };
  if (periodo === "semana") return { inicio: referencia, fim: adicionarDias(referencia, 6) };
  if (periodo === "personalizado") return personalizado;
  return { inicio: referencia, fim: referencia };
}

function proximoAtendimento(agendamentos: AgendamentoDaEquipe[]): AgendamentoDaEquipe | undefined {
  const agora = montarDataHora(hoje(), horaAtual());
  return agendamentos.find((a) => a.dataHora >= agora && !FORA_DO_PROXIMO.includes(a.status));
}

export function PainelInicio() {
  const [periodo, setPeriodo] = useState<Periodo>("hoje");
  const [personalizado, setPersonalizado] = useState(() => ({ inicio: hoje(), fim: adicionarDias(hoje(), 6) }));
  const [filtro, setFiltro] = useState<StatusAgendamento | null>(null);
  const [rotina, setRotina] = useState<{ executando: boolean; mensagem: string | null; erro: string | null }>({
    executando: false,
    mensagem: null,
    erro: null,
  });

  const { inicio, fim } = intervalo(periodo, personalizado);
  const consulta = useConsulta(() => listarAgendamentos({ inicio, fim }), `${inicio}:${fim}`, INTERVALO);
  const lista = consulta.dados;

  const filtrados = useMemo(
    () => (lista ? lista.agendamentos.filter((a) => !filtro || a.status === filtro) : []),
    [lista, filtro],
  );
  const proximo = lista ? proximoAtendimento(lista.agendamentos) : undefined;

  async function rodarRotina() {
    setRotina({ executando: true, mensagem: null, erro: null });
    try {
      const { mensagensEnviadas } = await executarRotinaDiaria();
      setRotina({
        executando: false,
        erro: null,
        mensagem:
          mensagensEnviadas === 0
            ? "Nenhum lembrete pendente para hoje."
            : `${mensagensEnviadas} ${mensagensEnviadas === 1 ? "mensagem enviada" : "mensagens enviadas"}.`,
      });
      consulta.recarregar();
    } catch (falha) {
      setRotina({ executando: false, mensagem: null, erro: mensagemDeErro(falha) });
    }
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo="Olá, equipe 👋" subtitulo="Acompanhe as confirmações dos pacientes pelo WhatsApp." />

      <div className={estilos.grade}>
        <div className={estilos.lateral}>
          <Cartao variante="destaque" espacamento="amplo" className={estilos.proximo}>
            {proximo ? (
              <>
                <div className={estilos.proximoTopo}>
                  <span className={estilos.proximoRotulo}>Próximo atendimento</span>
                  <EtiquetaStatus status={proximo.status} />
                </div>
                <p className={estilos.proximoNome}>{proximo.nomePaciente ?? "Paciente sem cadastro"}</p>
                <p className={estilos.proximoUnidade}>{proximo.unidade}</p>
                <p className={estilos.proximoQuando}>
                  <span>
                    <CalendarDays size={16} aria-hidden="true" /> {formatarDataCurta(proximo.dataHora)}, {nomeDiaCurto(proximo.dataHora)}
                  </span>
                  <span>
                    <Clock size={16} aria-hidden="true" /> {horaDe(proximo.dataHora)}
                  </span>
                </p>
                <Link href={`/agendamentos/${proximo.id}`} className={estilos.proximoLink}>
                  Ver agendamento <ArrowRight size={18} aria-hidden="true" />
                </Link>
              </>
            ) : (
              <>
                <span className={estilos.proximoRotulo}>Próximo atendimento</span>
                <p className={estilos.proximoNome}>{consulta.carregando && !lista ? "Carregando…" : "Nenhum atendimento pela frente no período"}</p>
                <Link href="/agendar" className={estilos.proximoLink}>
                  Agendar consulta <ArrowRight size={18} aria-hidden="true" />
                </Link>
              </>
            )}
          </Cartao>

          <section aria-labelledby="titulo-resumo" className={estilos.secao}>
            <h2 id="titulo-resumo" className={estilos.tituloSecao}>
              Resumo do período
            </h2>
            <div className={estilos.resumo}>
              {RESUMO.map((status) => (
                <button
                  key={status}
                  type="button"
                  aria-pressed={filtro === status}
                  className={classes(estilos.contador, estilos[status], filtro === status && estilos.contadorAtivo)}
                  onClick={() => setFiltro(filtro === status ? null : status)}
                >
                  <strong>{lista?.porStatus[status] ?? "–"}</strong>
                  <span>{STATUS[status].rotulo}</span>
                </button>
              ))}
            </div>
          </section>

          <section aria-labelledby="titulo-acoes" className={estilos.secao}>
            <h2 id="titulo-acoes" className={estilos.tituloSecao}>
              O que você precisa?
            </h2>
            <div className={estilos.acoes}>
              <Link href="/agendar" className={estilos.atalho}>
                <CalendarPlus size={24} aria-hidden="true" />
                Agendar consulta
              </Link>
              <button type="button" className={estilos.atalho} onClick={rodarRotina} disabled={rotina.executando}>
                <Send size={24} aria-hidden="true" />
                {rotina.executando ? "Enviando lembretes…" : "Enviar lembretes do dia"}
              </button>
            </div>
            {rotina.mensagem && <Alerta tom="sucesso">{rotina.mensagem}</Alerta>}
            {rotina.erro && <Alerta tom="perigo">{rotina.erro}</Alerta>}
          </section>
        </div>

        <section aria-labelledby="titulo-agendamentos" className={estilos.principal}>
          <div className={estilos.topoLista}>
            <h2 id="titulo-agendamentos" className={estilos.tituloSecao}>
              Agendamentos
            </h2>
            <div className={estilos.topoAcoes}>
              <AtualizadoHa atualizadoEm={consulta.atualizadoEm} carregando={consulta.carregando} aoAtualizar={consulta.recarregar} />
              <Link href="/agendamentos" className={estilos.verTodos}>
                Ver todos <ArrowRight size={16} aria-hidden="true" />
              </Link>
            </div>
          </div>

          <div className={estilos.periodos} role="group" aria-label="Período">
            {PERIODOS.map((p) => (
              <Chip key={p.valor} selecionado={periodo === p.valor} onClick={() => setPeriodo(p.valor)}>
                {p.rotulo}
              </Chip>
            ))}
          </div>

          {periodo === "personalizado" && (
            <div className={estilos.datas}>
              <Campo
                rotulo="De"
                type="date"
                value={personalizado.inicio}
                onChange={(e) => e.target.value && setPersonalizado({ ...personalizado, inicio: e.target.value })}
              />
              <Campo
                rotulo="Até"
                type="date"
                value={personalizado.fim}
                min={personalizado.inicio}
                onChange={(e) => e.target.value && setPersonalizado({ ...personalizado, fim: e.target.value })}
                dica="Até 31 dias"
              />
            </div>
          )}

          {filtro && (
            <Chip selecionado onClick={() => setFiltro(null)} className={estilos.filtroAtivo} aria-label={`Remover filtro ${STATUS[filtro].rotulo}`}>
              {STATUS[filtro].rotulo}
              <X size={14} aria-hidden="true" />
            </Chip>
          )}

          {consulta.erro ? (
            <Alerta tom="perigo" acao={<Botao tamanho="pequeno" variante="contorno" onClick={consulta.recarregar}>Tentar de novo</Botao>}>
              {mensagemDeErro(consulta.erro)}
            </Alerta>
          ) : !lista ? (
            <Carregando />
          ) : filtrados.length === 0 ? (
            <EstadoVazio
              icone={filtro ? <Inbox size={24} aria-hidden="true" /> : <CalendarCheck2 size={24} aria-hidden="true" />}
              titulo={filtro ? `Nenhum agendamento "${STATUS[filtro].rotulo}"` : "Nenhum agendamento no período"}
              descricao="Os agendamentos aparecem aqui e o status muda sozinho quando o paciente responde no WhatsApp."
              acao={
                <BotaoLink href="/agendar" variante="secundario" tamanho="pequeno">
                  Agendar consulta
                </BotaoLink>
              }
            />
          ) : (
            <ListaAgendamentos agendamentos={filtrados} agruparPorDia={inicio !== fim} />
          )}
        </section>
      </div>
    </div>
  );
}
