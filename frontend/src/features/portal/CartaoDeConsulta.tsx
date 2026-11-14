"use client";

import { CalendarClock, CalendarPlus, ChevronDown, ChevronUp, MapPin, Navigation, Stethoscope, XCircle } from "lucide-react";
import { useState } from "react";
import { cancelarConsulta } from "@/features/assistencial/api";
import { registrarMotivo } from "@/features/faltas/api";
import { TIPOS_DE_ATENDIMENTO } from "@/features/agendamentos/rotulos";
import { MOTIVOS, ORDEM_DOS_MOTIVOS } from "@/features/faltas/rotulos";
import type { MotivoDeAusencia } from "@/features/faltas/tipos";
import { NATUREZAS, STATUS_DA_CONSULTA, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import { useAcao } from "@/shared/hooks/useAcao";
import { classes } from "@/shared/lib/classes";
import { dataDe, diaDoMes, formatarDataCurta, horaDe, nomeDiaCurto } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { Chip } from "@/shared/ui/Chip";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { type ConsultaDoPaciente, linkComoChegar, linkDeRemarcacao, linkParaAgenda, podeCancelar, podeRemarcar } from "./consultas";
import { AnotacoesDaConsulta } from "./AnotacoesDaConsulta";
import estilos from "./CartaoDeConsulta.module.css";

type Props = {
  item: ConsultaDoPaciente;
  nomeProfissional?: string;
  destaque?: boolean;
  aoAlterar: () => void;
  comAnotacoes?: boolean;
};

export function CartaoDeConsulta({ item, nomeProfissional, destaque = false, aoAlterar, comAnotacoes = false }: Props) {
  const { consulta, encaminhamento } = item;
  const [confirmando, setConfirmando] = useState(false);
  const [motivo, setMotivo] = useState<MotivoDeAusencia | null>(null);
  const [vendoAnotacoes, setVendoAnotacoes] = useState(false);
  const temAnotacoes = comAnotacoes && consulta.status === "COMPARECEU" && consulta.triagemId !== null;
  const acao = useAcao();
  const dia = dataDe(consulta.inicio);
  const titulo = encaminhamento
    ? nomeDaEspecialidade(encaminhamento.especialidade)
    : consulta.tipo === "EXAME"
      ? TIPOS_DE_ATENDIMENTO.EXAME
      : NATUREZAS[consulta.natureza];

  async function cancelar() {
    const cancelada = await acao.executar("cancelar", async () => {
      const resultado = await cancelarConsulta(consulta.id, consulta.versao);
      if (motivo) await registrarMotivo(consulta.id, motivo).catch(() => undefined);
      return resultado;
    });
    if (cancelada) {
      setConfirmando(false);
      aoAlterar();
    }
  }

  return (
    <article className={classes(estilos.cartao, destaque && estilos.destaque)}>
      <div className={estilos.data} aria-hidden="true">
        <span className={estilos.semana}>{nomeDiaCurto(dia)}</span>
        <span className={estilos.numero}>{diaDoMes(dia)}</span>
        <span className={estilos.mes}>{formatarDataCurta(dia).split(" ")[1]}</span>
      </div>

      <div className={estilos.corpo}>
        <div className={estilos.topo}>
          <h3 className={estilos.titulo}>{titulo}</h3>
          <Etiqueta tom={STATUS_DA_CONSULTA[consulta.status].tom}>{STATUS_DA_CONSULTA[consulta.status].texto}</Etiqueta>
        </div>
        <p className={estilos.quando}>
          <CalendarClock size={16} aria-hidden="true" />
          {formatarDataCurta(dia)}, às {horaDe(consulta.inicio)}
          {encaminhamento && <span className={estilos.natureza}>{NATUREZAS[consulta.natureza]}</span>}
        </p>
        {nomeProfissional && (
          <p className={estilos.linha}>
            <Stethoscope size={16} aria-hidden="true" />
            {nomeProfissional}
          </p>
        )}
        <p className={estilos.linha}>
          <MapPin size={16} aria-hidden="true" />
          {consulta.unidade} · {consulta.endereco}
        </p>
        {podeCancelar(item) && (
          <p className={estilos.atalhos}>
            <a href={linkComoChegar(item)} target="_blank" rel="noopener noreferrer">
              <Navigation size={14} aria-hidden="true" />
              Como chegar
            </a>
            <a href={linkParaAgenda(item, titulo)} target="_blank" rel="noopener noreferrer">
              <CalendarPlus size={14} aria-hidden="true" />
              Salvar na agenda do celular
            </a>
          </p>
        )}

        {temAnotacoes && consulta.triagemId && (
          <>
            <Botao
              variante="fantasma"
              tamanho="pequeno"
              className={estilos.alternar}
              icone={vendoAnotacoes ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
              aria-expanded={vendoAnotacoes}
              onClick={() => setVendoAnotacoes((atual) => !atual)}
            >
              {vendoAnotacoes ? "Ocultar anotações" : "Ver anotações do profissional"}
            </Botao>
            {vendoAnotacoes && <AnotacoesDaConsulta triagemId={consulta.triagemId} consultaId={consulta.id} />}
          </>
        )}

        {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

        {(podeRemarcar(item) || podeCancelar(item)) && !confirmando && (
          <div className={estilos.acoes}>
            {podeRemarcar(item) && (
              <BotaoLink href={linkDeRemarcacao(item)} variante="contorno" tamanho="pequeno" icone={<CalendarClock size={16} aria-hidden="true" />}>
                Remarcar
              </BotaoLink>
            )}
            {podeCancelar(item) && (
              <Botao variante="fantasma" tamanho="pequeno" icone={<XCircle size={16} aria-hidden="true" />} onClick={() => setConfirmando(true)}>
                Cancelar
              </Botao>
            )}
          </div>
        )}

        {confirmando && (
          <div className={estilos.confirmar}>
            <p>Cancelar esta consulta? O horário fica livre para outro paciente.</p>
            <fieldset className={estilos.motivos}>
              <legend>Se quiser, conte o motivo. Isso ajuda a unidade a melhorar.</legend>
              <div className={estilos.opcoes}>
                {ORDEM_DOS_MOTIVOS.map((m) => (
                  <Chip key={m} selecionado={motivo === m} onClick={() => setMotivo(motivo === m ? null : m)}>
                    {MOTIVOS[m].rotulo}
                  </Chip>
                ))}
              </div>
            </fieldset>
            <div className={estilos.acoes}>
              <Botao
                variante="contorno"
                tamanho="pequeno"
                onClick={() => {
                  setConfirmando(false);
                  setMotivo(null);
                }}
              >
                Manter consulta
              </Botao>
              <Botao variante="perigo" tamanho="pequeno" carregando={acao.executando === "cancelar"} onClick={cancelar}>
                Confirmar cancelamento
              </Botao>
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
