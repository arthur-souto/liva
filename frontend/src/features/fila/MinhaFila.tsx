"use client";

import { CalendarCheck, ListOrdered } from "lucide-react";
import { useState } from "react";
import type { Consulta } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { formatarDataHora } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Secao } from "@/shared/ui/Secao";
import { CartaoDeOferta } from "./CartaoDeOferta";
import { JornadaNaFila } from "./JornadaNaFila";
import { PosicaoNoRamo } from "./PosicaoNoRamo";
import { useMinhaFila } from "./useMinhaFila";
import estilos from "./MinhaFila.module.css";

export function MinhaFila() {
  const fila = useMinhaFila();
  const [aceita, setAceita] = useState<Consulta | null>(null);
  const [recusou, setRecusou] = useState(false);
  const dados = fila.dados;
  const ativas = dados?.filas.filter((f) => f.status === "AGUARDANDO" || f.status === "PAUSADA") ?? [];
  const recentes = dados?.filas.filter((f) => f.status === "ATENDIDA" || f.status === "ENCERRADA") ?? [];

  function aoAceitar(consulta: Consulta) {
    setAceita(consulta);
    setRecusou(false);
    fila.recarregar();
  }

  function aoRecusar() {
    setRecusou(true);
    setAceita(null);
    fila.recarregar();
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Minha fila"
        subtitulo="Sua posição em cada encaminhamento. Quando um horário mais cedo ficar livre, avisamos aqui e pelo WhatsApp."
      />
      {fila.erro ? <Alerta tom="perigo">{mensagemDeErro(fila.erro)}</Alerta> : null}
      {!dados && !fila.erro && (
        <>
          <Carregando linhas={1} />
          <Carregando linhas={2} />
        </>
      )}

      {aceita && (
        <Alerta
          tom="sucesso"
          titulo="Vaga garantida!"
          acao={
            <BotaoLink href="/portal/consultas" variante="contorno" tamanho="pequeno">
              Ver minhas consultas
            </BotaoLink>
          }
        >
          Sua consulta ficou para {formatarDataHora(aceita.inicio)} e já está confirmada.
        </Alerta>
      )}
      {recusou && <Alerta tom="info">Recusa registrada. Você continua na fila na mesma posição.</Alerta>}

      {fila.ofertas.map((o) => (
        <CartaoDeOferta key={o.id} oferta={o} agora={fila.agora} aoAceitar={aoAceitar} aoRecusar={aoRecusar} />
      ))}

      {dados && (
        <Secao titulo="Filas em que você está" descricao="A ordem segue a prioridade definida na triagem e, depois, a data de entrada.">
          {ativas.length === 0 ? (
            <EstadoVazio
              icone={<ListOrdered size={28} aria-hidden="true" />}
              titulo="Você não está em nenhuma fila agora"
              descricao="Quando um encaminhamento for liberado, você entra na fila da especialidade e acompanha a sua posição aqui."
            />
          ) : (
            <ul className={estilos.lista}>
              {ativas.map((f) => (
                <li key={f.entradaId}>
                  {f.status === "AGUARDANDO" && f.posicao !== null ? (
                    <JornadaNaFila fila={f} agora={fila.agora} />
                  ) : (
                    <PosicaoNoRamo fila={f} />
                  )}
                </li>
              ))}
            </ul>
          )}
        </Secao>
      )}

      {recentes.length > 0 && (
        <Secao titulo="Últimos 30 dias" descricao="Filas das quais você saiu recentemente.">
          <ul className={estilos.lista}>
            {recentes.map((f) => (
              <li key={f.entradaId}>
                <PosicaoNoRamo fila={f} />
              </li>
            ))}
          </ul>
        </Secao>
      )}

      {dados && (
        <div className={estilos.comoFunciona}>
          <CalendarCheck size={20} aria-hidden="true" />
          <p>
            Se alguém desmarcar, o horário é oferecido para quem está à frente na fila. Você tem um prazo para aceitar. Recusar ou deixar o
            prazo passar não tira você da fila.
          </p>
        </div>
      )}
    </div>
  );
}
