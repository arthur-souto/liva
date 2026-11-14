"use client";

import { CalendarClock, MapPin, Stethoscope, XCircle } from "lucide-react";
import { useState } from "react";
import { cancelarConsulta } from "@/features/assistencial/api";
import { NATUREZAS, STATUS_DA_CONSULTA, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import { useAcao } from "@/shared/hooks/useAcao";
import { classes } from "@/shared/lib/classes";
import { dataDe, diaDoMes, formatarDataCurta, horaDe, nomeDiaCurto } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { type ConsultaDoPaciente, linkDeRemarcacao, podeCancelar, podeRemarcar } from "./consultas";
import estilos from "./CartaoDeConsulta.module.css";

type Props = {
  item: ConsultaDoPaciente;
  nomeProfissional?: string;
  destaque?: boolean;
  aoAlterar: () => void;
};

export function CartaoDeConsulta({ item, nomeProfissional, destaque = false, aoAlterar }: Props) {
  const { consulta, encaminhamento } = item;
  const [confirmando, setConfirmando] = useState(false);
  const acao = useAcao();
  const dia = dataDe(consulta.inicio);
  const titulo = encaminhamento ? nomeDaEspecialidade(encaminhamento.especialidade) : NATUREZAS[consulta.natureza];

  async function cancelar() {
    if (await acao.executar("cancelar", () => cancelarConsulta(consulta.id, consulta.versao))) {
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
            <div className={estilos.acoes}>
              <Botao variante="contorno" tamanho="pequeno" onClick={() => setConfirmando(false)}>
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
