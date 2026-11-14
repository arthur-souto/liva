"use client";

import { MessageCircle, Send } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useEffect, useRef, useState } from "react";
import { STATUS } from "@/features/agendamentos/rotulos";
import { FalhaApi, mensagemDeErro } from "@/shared/api/erro";
import { classes } from "@/shared/lib/classes";
import { horaAtual } from "@/shared/lib/datas";
import { apenasDigitos, formatarTelefone, mascararTelefone, telefoneValido } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Cartao } from "@/shared/ui/Cartao";
import { Chip } from "@/shared/ui/Chip";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Segmentado } from "@/shared/ui/Segmentado";
import { enviarMensagem, type ResultadoResposta, simularResposta } from "./api";
import estilos from "./SimuladorConversa.module.css";

type Autor = "paciente" | "unidade" | "sistema";
type Modo = "paciente" | "unidade";

type Mensagem = {
  id: number;
  autor: Autor;
  texto: string;
  hora: string;
  falhou?: boolean;
  agendamentoId?: string | null;
};

const RESPOSTAS_RAPIDAS = [
  { texto: "1", rotulo: "1 · Vou" },
  { texto: "2", rotulo: "2 · Não vou" },
  { texto: "3", rotulo: "3 · Outro horário" },
];

const MODOS: { valor: Modo; rotulo: string }[] = [
  { valor: "paciente", rotulo: "Responder como paciente" },
  { valor: "unidade", rotulo: "Enviar como unidade" },
];

function descreverResultado(resultado: ResultadoResposta): string {
  if (resultado.aplicada && resultado.status) {
    const retorno = resultado.retornoEnviado ? "Retorno enviado no WhatsApp." : "Falha ao enviar o retorno.";
    return `Status: ${STATUS[resultado.status].rotulo}. ${retorno}`;
  }
  return resultado.motivo ?? "Resposta não aplicada.";
}

export function SimuladorConversa({ telefoneInicial }: { telefoneInicial?: string }) {
  const [telefone, setTelefone] = useState(() => mascararTelefone(telefoneInicial ?? ""));
  const [modo, setModo] = useState<Modo>("paciente");
  const [texto, setTexto] = useState("");
  const [mensagens, setMensagens] = useState<Mensagem[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [simulacaoDesligada, setSimulacaoDesligada] = useState(false);
  const proximoId = useRef(0);
  const fim = useRef<HTMLDivElement>(null);
  const valido = telefoneValido(telefone);

  useEffect(() => {
    fim.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [mensagens]);

  function adicionar(...novas: Omit<Mensagem, "id" | "hora">[]) {
    setMensagens((anteriores) => [...anteriores, ...novas.map((m) => ({ ...m, id: proximoId.current++, hora: horaAtual() }))]);
  }

  async function mandar(conteudo: string) {
    const limpo = conteudo.trim();
    if (!limpo || !valido || enviando) return;
    const numero = apenasDigitos(telefone);
    setEnviando(true);
    setTexto("");
    try {
      if (modo === "paciente") {
        adicionar({ autor: "paciente", texto: limpo });
        const resultado = await simularResposta(numero, limpo);
        adicionar({ autor: "sistema", texto: descreverResultado(resultado), agendamentoId: resultado.agendamentoId });
      } else {
        await enviarMensagem(numero, limpo);
        adicionar({ autor: "unidade", texto: limpo });
      }
    } catch (erro) {
      if (modo === "paciente" && erro instanceof FalhaApi && erro.codigo === "NAO_ENCONTRADO") setSimulacaoDesligada(true);
      else adicionar(modo === "unidade" ? { autor: "unidade", texto: limpo, falhou: true } : { autor: "sistema", texto: mensagemDeErro(erro), falhou: true });
    } finally {
      setEnviando(false);
    }
  }

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault();
    mandar(texto);
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo="Conversas" subtitulo="Simule as respostas do paciente no WhatsApp" />

      <Cartao className={estilos.configuracao}>
        <Campo
          rotulo="WhatsApp do paciente"
          type="tel"
          inputMode="numeric"
          value={telefone}
          onChange={(e) => setTelefone(mascararTelefone(e.target.value))}
          placeholder="(51) 99999-9999"
          dica="Use o número de um agendamento aguardando resposta."
        />
        <Segmentado rotulo="Modo da conversa" opcoes={MODOS} valor={modo} aoMudar={setModo} />
      </Cartao>

      {simulacaoDesligada && modo === "paciente" && (
        <Alerta tom="alerta" titulo="Simulação desativada">
          Os endpoints de teste estão desligados no backend (confirmasus.atendimento-teste.habilitado=false).
        </Alerta>
      )}

      <section className={estilos.chat} aria-label="Conversa">
        <div className={estilos.topoChat}>
          <span className={estilos.avatar} aria-hidden="true">
            {modo === "paciente" ? "P" : "L"}
          </span>
          <div>
            <p className={estilos.nomeChat}>{valido ? formatarTelefone(telefone) : "Paciente"}</p>
            <p className={estilos.subChat}>{modo === "paciente" ? "Você está respondendo como o paciente" : "Mensagem real pelo WhatsApp"}</p>
          </div>
        </div>

        <div className={estilos.mensagens} aria-live="polite">
          {mensagens.length === 0 ? (
            <EstadoVazio
              icone={<MessageCircle size={28} />}
              titulo="Nenhuma mensagem ainda"
              descricao={modo === "paciente" ? "Toque em 1, 2 ou 3 para responder ao pedido de confirmação." : "Escreva uma mensagem para o paciente."}
            />
          ) : (
            mensagens.map((m) => (
              <div key={m.id} className={classes(estilos.balao, estilos[m.autor], m.falhou && estilos.falhou)}>
                <p>{m.texto}</p>
                {m.agendamentoId && (
                  <Link href={`/agendamentos/${m.agendamentoId}`} className={estilos.link}>
                    Ver agendamento
                  </Link>
                )}
                <span className={estilos.hora}>{m.falhou && m.autor === "unidade" ? "Não enviada" : m.hora}</span>
              </div>
            ))
          )}
          <div ref={fim} />
        </div>

        {modo === "paciente" && (
          <div className={estilos.rapidas}>
            {RESPOSTAS_RAPIDAS.map((r) => (
              <Chip key={r.texto} variante="realce" onClick={() => mandar(r.texto)} disabled={!valido || enviando}>
                {r.rotulo}
              </Chip>
            ))}
          </div>
        )}

        <form className={estilos.entrada} onSubmit={aoEnviar}>
          <label htmlFor="mensagem" className="somente-leitor-de-tela">
            Mensagem
          </label>
          <input
            id="mensagem"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder={valido ? "Escreva uma mensagem" : "Informe o WhatsApp acima"}
            disabled={!valido}
            autoComplete="off"
          />
          <button type="submit" className={estilos.enviar} disabled={!valido || !texto.trim() || enviando} aria-label="Enviar">
            <Send size={18} aria-hidden="true" />
          </button>
        </form>
      </section>
    </div>
  );
}
