"use client";

import { CalendarCheck2, ChevronLeft, ChevronRight, Clock } from "lucide-react";
import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { adicionarDias, dataDe, formatarDataCurta, horaDe, hoje, rotuloDoDia } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Carregando } from "@/shared/ui/Carregando";
import { Chip } from "@/shared/ui/Chip";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { agendarConsulta, horariosDisponiveis } from "../api";
import type { Consulta, HorarioDisponivel } from "../tipos";
import estilos from "./EscolhaDeHorario.module.css";

const DIAS_POR_PAGINA = 7;

type Escolha = { horario: HorarioDisponivel; chave: string };

function agruparPorDia(horarios: HorarioDisponivel[]): [string, HorarioDisponivel[]][] {
  const grupos = new Map<string, HorarioDisponivel[]>();
  horarios.forEach((h) => grupos.set(dataDe(h.inicio), [...(grupos.get(dataDe(h.inicio)) ?? []), h]));
  return [...grupos.entries()];
}

function mesmoHorario(a: HorarioDisponivel, b: HorarioDisponivel): boolean {
  return a.inicio === b.inicio && a.profissionalId === b.profissionalId;
}

export function EscolhaDeHorario({ encaminhamentoId, aoAgendar }: { encaminhamentoId: string; aoAgendar: (consulta: Consulta) => void }) {
  const [inicio, setInicio] = useState(hoje);
  const [escolha, setEscolha] = useState<Escolha | null>(null);
  const fim = adicionarDias(inicio, DIAS_POR_PAGINA - 1);
  const horarios = useConsulta(() => horariosDisponiveis(encaminhamentoId, inicio, fim), `${encaminhamentoId}-${inicio}`);
  const acao = useAcao();

  function mudarSemana(dias: number) {
    setInicio((atual) => adicionarDias(atual, dias));
    setEscolha(null);
  }

  async function confirmar() {
    if (!escolha) return;
    const consulta = await acao.executar("agendar", () =>
      agendarConsulta(encaminhamentoId, escolha.horario.profissionalId, escolha.horario.inicio, escolha.chave),
    );
    if (consulta) aoAgendar(consulta);
    else {
      setEscolha(null);
      horarios.recarregar();
    }
  }

  const grupos = agruparPorDia(horarios.dados ?? []);

  return (
    <div className={estilos.escolha}>
      <div className={estilos.navegacao}>
        <Botao variante="fantasma" tamanho="pequeno" icone={<ChevronLeft size={18} aria-hidden="true" />} onClick={() => mudarSemana(-DIAS_POR_PAGINA)} disabled={inicio <= hoje()}>
          Anterior
        </Botao>
        <span className={estilos.periodo}>
          {formatarDataCurta(inicio)} a {formatarDataCurta(fim)}
        </span>
        <Botao variante="fantasma" tamanho="pequeno" iconeDireita={<ChevronRight size={18} aria-hidden="true" />} onClick={() => mudarSemana(DIAS_POR_PAGINA)}>
          Próxima
        </Botao>
      </div>

      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      {horarios.erro ? <Alerta tom="perigo">{mensagemDeErro(horarios.erro)}</Alerta> : null}
      {horarios.carregando && !horarios.dados && <Carregando />}
      {horarios.dados && grupos.length === 0 && (
        <EstadoVazio icone={<Clock size={28} aria-hidden="true" />} titulo="Nenhum horário livre neste período" descricao="Veja a próxima semana." />
      )}

      {grupos.map(([dia, doDia]) => (
        <section key={dia} className={estilos.dia}>
          <h3 className={estilos.rotuloDia}>{rotuloDoDia(dia)}</h3>
          <div className={estilos.horarios}>
            {doDia.map((h) => (
              <Chip
                key={`${h.profissionalId}-${h.inicio}`}
                selecionado={escolha !== null && mesmoHorario(escolha.horario, h)}
                onClick={() => setEscolha({ horario: h, chave: crypto.randomUUID() })}
                aria-label={`${horaDe(h.inicio)} com ${h.nomeProfissional}`}
              >
                <span className={estilos.hora}>{horaDe(h.inicio)}</span>
                <span className={estilos.profissional}>{h.nomeProfissional}</span>
              </Chip>
            ))}
          </div>
        </section>
      ))}

      {escolha && (
        <div className={estilos.confirmacao} role="region" aria-label="Confirmar horário">
          <p>
            <strong>{rotuloDoDia(dataDe(escolha.horario.inicio))}</strong>, às {horaDe(escolha.horario.inicio)} com{" "}
            {escolha.horario.nomeProfissional}
          </p>
          <Botao icone={<CalendarCheck2 size={18} aria-hidden="true" />} carregando={acao.executando === "agendar"} onClick={confirmar}>
            Confirmar agendamento
          </Botao>
        </div>
      )}
    </div>
  );
}
