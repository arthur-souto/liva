"use client";

import { useEffect, useMemo, useState } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { rotaPermitida } from "@/shared/sessao/acesso";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Banner } from "@/shared/ui/Banner";
import { Guia } from "@/shared/ui/Guia";
import { ProfissionalComPrancheta } from "@/shared/ui/Ilustracoes";
import { FluxoDoPaciente } from "./FluxoDoPaciente";
import { AREA_DO_PAPEL, AREAS, type AreaDoGuia } from "./topicos";
import estilos from "./GuiaDaEquipe.module.css";

function areaDoEndereco(): { area: AreaDoGuia; topico: string } | null {
  const alvo = window.location.hash.slice(1);
  const area = AREAS.find(({ id, topicos }) => id === alvo || topicos.some((topico) => topico.id === alvo));
  return area && alvo ? { area: area.id, topico: alvo } : null;
}

export function GuiaDaEquipe() {
  const usuario = useConsulta(usuarioAtual, "usuario");
  const papel = usuario.dados?.papel;
  const [escolhida, setEscolhida] = useState<AreaDoGuia | null>(null);
  const area = escolhida ?? (papel ? AREA_DO_PAPEL[papel] : "recepcao");
  const atual = AREAS.find(({ id }) => id === area) ?? AREAS[0];

  const topicos = useMemo(
    () =>
      atual.topicos.map((topico) =>
        topico.link && papel && !rotaPermitida(papel, topico.link.href) ? { ...topico, link: undefined } : topico,
      ),
    [atual, papel],
  );

  useEffect(() => {
    const destino = areaDoEndereco();
    if (!destino) return;
    setEscolhida(destino.area);
    requestAnimationFrame(() => document.getElementById(destino.topico)?.scrollIntoView({ block: "start" }));
  }, []);

  function escolher(proxima: AreaDoGuia) {
    setEscolhida(proxima);
    window.history.replaceState(null, "", `#${proxima}`);
  }

  return (
    <Guia
      rotulo={`Assuntos de ${atual.rotulo}`}
      topicos={topicos}
      cabecalho={
        <Banner titulo="Como usar a Liva" ilustracao={<ProfissionalComPrancheta />}>
          O passo a passo de cada tela, para quem trabalha na unidade. Comece pela sua área ou explore as outras para entender o caminho
          inteiro do paciente.
        </Banner>
      }
      rodape={
        <Alerta tom="info" titulo="A Liva registra, não substitui">
          A triagem e as consultas acontecem presencialmente. A Liva registra o que foi feito, organiza a fila e conversa com o paciente
          pelo WhatsApp.
        </Alerta>
      }
    >
      <FluxoDoPaciente area={area} aoEscolher={escolher} />

      <div className={estilos.areas} role="group" aria-label="Áreas do guia">
        {AREAS.map(({ id, rotulo, Icone, descricao, topicos: assuntos }) => (
          <button
            key={id}
            type="button"
            aria-pressed={id === area}
            className={classes(estilos.area, id === area && estilos.ativa)}
            onClick={() => escolher(id)}
          >
            <span className={estilos.iconeArea} aria-hidden="true">
              <Icone size={20} />
            </span>
            <span className={estilos.textoArea}>
              <strong>
                {rotulo}
                {papel && AREA_DO_PAPEL[papel] === id && <span className={estilos.sua}>Sua área</span>}
              </strong>
              <span>{descricao}</span>
              <span className={estilos.contagem}>{assuntos.length} assuntos</span>
            </span>
          </button>
        ))}
      </div>

      <h2 key={area} className={estilos.tituloArea}>
        <atual.Icone size={22} aria-hidden="true" />
        {atual.rotulo}
      </h2>
    </Guia>
  );
}
