"use client";

import { ArrowLeft, CalendarDays, CalendarSearch, Clock, MapPin, MessageCircle } from "lucide-react";
import { FalhaApi, mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { dataDe, formatarDiaLongo, horaDe } from "@/shared/lib/datas";
import { apenasDigitos, formatarTelefone, linkWhatsApp } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Cartao } from "@/shared/ui/Cartao";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { buscarAgendamento, historicoDeMensagens, listarAgendamentos } from "../api";
import { ACAO_DA_EQUIPE, TIPOS_DE_ATENDIMENTO } from "../rotulos";
import { AcoesDeLembrete } from "./AcoesDeLembrete";
import { AndamentoDaConfirmacao } from "./AndamentoDaConfirmacao";
import { EtiquetaStatus } from "./EtiquetaStatus";
import { HistoricoMensagens } from "./HistoricoMensagens";
import estilos from "./DetalheAgendamento.module.css";

const INTERVALO = 10000;

export function DetalheAgendamento({ id }: { id: string }) {
  const consulta = useConsulta(() => buscarAgendamento(id), `agendamento-${id}`, INTERVALO);
  const historico = useConsulta(() => historicoDeMensagens(id), `historico-${id}`, INTERVALO);
  const agendamento = consulta.dados;
  const dia = agendamento ? dataDe(agendamento.dataHora) : null;
  const paciente = useConsulta(
    async () => (dia ? ((await listarAgendamentos({ inicio: dia, fim: dia })).agendamentos.find((a) => a.id === id) ?? null) : null),
    `paciente-${id}-${dia}`,
  );

  const voltar = (
    <BotaoLink href="/agendamentos" variante="fantasma" tamanho="pequeno" icone={<ArrowLeft size={16} aria-hidden="true" />}>
      Voltar aos agendamentos
    </BotaoLink>
  );

  if (!agendamento) {
    const naoExiste = consulta.erro instanceof FalhaApi && ["NAO_ENCONTRADO", "REQUISICAO_INVALIDA"].includes(consulta.erro.codigo);
    return (
      <div className={estilos.pagina}>
        {voltar}
        {naoExiste ? (
          <EstadoVazio
            icone={<CalendarSearch size={24} aria-hidden="true" />}
            titulo="Agendamento não encontrado"
            descricao="Confira o link ou procure o agendamento no painel."
          />
        ) : consulta.erro ? (
          <Alerta tom="perigo" acao={<Botao tamanho="pequeno" variante="contorno" onClick={consulta.recarregar}>Tentar de novo</Botao>}>
            {mensagemDeErro(consulta.erro)}
          </Alerta>
        ) : (
          <Carregando linhas={3} />
        )}
      </div>
    );
  }

  const dados = paciente.dados;
  const acao = ACAO_DA_EQUIPE[agendamento.status];
  const ultimaResposta = historico.dados?.findLast((m) => m.direcao === "RECEBIDA");

  return (
    <div className={estilos.pagina}>
      {voltar}
      <CabecalhoPagina
        titulo={dados?.nomePaciente ?? "Agendamento"}
        subtitulo={`${TIPOS_DE_ATENDIMENTO[agendamento.tipo]} · ${formatarDiaLongo(agendamento.dataHora)}`}
        acoes={<AtualizadoHa atualizadoEm={consulta.atualizadoEm} carregando={consulta.carregando} aoAtualizar={consulta.recarregar} />}
      />

      <div className={estilos.grade}>
        <div className={estilos.coluna}>
          <Cartao variante="destaque" espacamento="amplo" className={estilos.resumo}>
            <div className={estilos.topoResumo}>
              <span className={estilos.rotuloResumo}>{TIPOS_DE_ATENDIMENTO[agendamento.tipo]}</span>
              <EtiquetaStatus status={agendamento.status} />
            </div>
            <p className={estilos.unidade}>{agendamento.unidade}</p>
            <p className={estilos.quando}>
              <span>
                <CalendarDays size={16} aria-hidden="true" /> {formatarDiaLongo(agendamento.dataHora)}
              </span>
              <span>
                <Clock size={16} aria-hidden="true" /> {horaDe(agendamento.dataHora)}
              </span>
            </p>
            {acao && <p className={estilos.acao}>{acao}</p>}
          </Cartao>

          <Cartao espacamento="amplo">
            <dl className={estilos.dados}>
              <div>
                <dt>Paciente</dt>
                <dd>{dados?.nomePaciente ?? "—"}</dd>
              </div>
              <div>
                <dt>Telefone</dt>
                <dd>
                  {dados?.telefone ? (
                    <a href={linkWhatsApp(dados.telefone)} target="_blank" rel="noreferrer" className={estilos.link}>
                      {formatarTelefone(dados.telefone)}
                    </a>
                  ) : (
                    "—"
                  )}
                </dd>
              </div>
              {dados?.nomeCuidador && (
                <div>
                  <dt>Cuidador</dt>
                  <dd>
                    {dados.nomeCuidador} · {formatarTelefone(dados.telefoneCuidador)}
                  </dd>
                </div>
              )}
              <div>
                <dt>Endereço</dt>
                <dd className={estilos.endereco}>
                  <MapPin size={16} aria-hidden="true" /> {agendamento.endereco}
                </dd>
              </div>
              <div>
                <dt>Preparo</dt>
                <dd>{agendamento.preparo ?? "Sem orientações de preparo"}</dd>
              </div>
            </dl>
          </Cartao>
        </div>

        <div className={estilos.coluna}>
          <Cartao espacamento="amplo" como="section" aria-labelledby="titulo-resposta">
            <h2 id="titulo-resposta" className={estilos.tituloSecao}>
              Confirmação do paciente
            </h2>
            <p className={estilos.descricaoSecao}>Atualiza sozinho quando o paciente responde no WhatsApp.</p>
            <AndamentoDaConfirmacao status={agendamento.status} ultimaResposta={ultimaResposta} />
          </Cartao>
          <Cartao espacamento="amplo" como="section" aria-labelledby="titulo-historico">
            <h2 id="titulo-historico" className={estilos.tituloSecao}>
              Histórico no WhatsApp
            </h2>
            <p className={estilos.descricaoSecao}>Tudo o que a Liva enviou e o que o paciente respondeu.</p>
            {historico.erro ? (
              <Alerta tom="perigo">{mensagemDeErro(historico.erro)}</Alerta>
            ) : historico.dados ? (
              <HistoricoMensagens mensagens={historico.dados} />
            ) : (
              <Carregando linhas={2} />
            )}
          </Cartao>
          <Cartao espacamento="amplo" como="section" aria-labelledby="titulo-lembretes">
            <h2 id="titulo-lembretes" className={estilos.tituloSecao}>
              Lembretes pelo WhatsApp
            </h2>
            <p className={estilos.descricaoSecao}>Reenvie uma mensagem para o paciente e o cuidador, se houver.</p>
            <AcoesDeLembrete
              agendamentoId={agendamento.id}
              aoEnviar={() => {
                consulta.recarregar();
                historico.recarregar();
              }}
            />
          </Cartao>
          {dados?.telefone && (
            <BotaoLink
              href={`/conversas?telefone=${apenasDigitos(dados.telefone)}`}
              variante="contorno"
              larguraTotal
              icone={<MessageCircle size={18} aria-hidden="true" />}
            >
              Simular resposta deste paciente
            </BotaoLink>
          )}
        </div>
      </div>
    </div>
  );
}
