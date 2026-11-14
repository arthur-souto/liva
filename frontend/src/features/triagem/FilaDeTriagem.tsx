"use client";

import { ArrowRight, Stethoscope, Users } from "lucide-react";
import { useRouter } from "next/navigation";
import { filaDeTriagem, iniciarTriagem } from "@/features/assistencial/api";
import { STATUS_DA_TRIAGEM } from "@/features/assistencial/rotulos";
import type { ItemDaFila } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { formatarInstante } from "@/shared/lib/datas";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { ProfissionalComPrancheta } from "@/shared/ui/Ilustracoes";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { LinkDeAjuda } from "@/shared/ui/LinkDeAjuda";
import estilos from "./FilaDeTriagem.module.css";

export function FilaDeTriagem() {
  const router = useRouter();
  const fila = useConsulta(filaDeTriagem, "fila-triagem", 10000);
  const usuario = useConsulta(usuarioAtual, "usuario");
  const acao = useAcao();
  const eu = usuario.dados?.profissionalId;

  async function atender(item: ItemDaFila) {
    if (await acao.executar(item.triagemId, () => iniciarTriagem(item.triagemId))) router.push(`/triagem/${item.triagemId}`);
    else fila.recarregar();
  }

  const minhas = (fila.dados ?? []).filter((i) => i.status === "EM_ANDAMENTO" && i.profissionalId === eu);
  const aguardando = (fila.dados ?? []).filter((i) => i.status === "AGUARDANDO_ATENDIMENTO");
  const comOutros = (fila.dados ?? []).filter((i) => i.status === "EM_ANDAMENTO" && i.profissionalId !== eu);

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Triagem"
        subtitulo="Registro da triagem feita na unidade. Pacientes por ordem de chegada; ao iniciar o registro, ele fica sob sua responsabilidade."
        acoes={<LinkDeAjuda topico="fila-triagem" />}
      />
      <div className={estilos.status}>
        <AtualizadoHa atualizadoEm={fila.atualizadoEm} carregando={fila.carregando} aoAtualizar={fila.recarregar} />
      </div>
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      {fila.erro ? <Alerta tom="perigo">{mensagemDeErro(fila.erro)}</Alerta> : null}
      {fila.carregando && !fila.dados && <Carregando linhas={4} />}

      {minhas.length > 0 && (
        <section className={estilos.grupo} aria-label="Em atendimento por você">
          <h2 className={estilos.titulo}>Em atendimento por você</h2>
          <ul className={estilos.lista}>
            {minhas.map((item) => (
              <li key={item.triagemId} className={classes(estilos.item, estilos.minha)}>
                <Dados item={item} />
                <BotaoLink href={`/triagem/${item.triagemId}`} tamanho="pequeno" iconeDireita={<ArrowRight size={16} aria-hidden="true" />}>
                  Continuar
                </BotaoLink>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className={estilos.grupo} aria-label="Aguardando triagem">
        <h2 className={estilos.titulo}>Aguardando ({aguardando.length})</h2>
        {fila.dados && aguardando.length === 0 && (
          <EstadoVazio
            ilustracao={<ProfissionalComPrancheta />}
            icone={<Users size={28} aria-hidden="true" />}
            titulo="Ninguém aguardando"
            descricao="A recepção registra a chegada dos pacientes."
          />
        )}
        <ol className={estilos.lista}>
          {aguardando.map((item, posicao) => (
            <li key={item.triagemId} className={estilos.item}>
              <span className={estilos.posicao}>{posicao + 1}</span>
              <Dados item={item} />
              <Botao
                tamanho="pequeno"
                icone={<Stethoscope size={16} aria-hidden="true" />}
                carregando={acao.executando === item.triagemId}
                onClick={() => atender(item)}
              >
                Registrar triagem
              </Botao>
            </li>
          ))}
        </ol>
      </section>

      {comOutros.length > 0 && (
        <section className={estilos.grupo} aria-label="Com outros profissionais">
          <h2 className={estilos.titulo}>Com outros profissionais</h2>
          <ul className={estilos.lista}>
            {comOutros.map((item) => (
              <li key={item.triagemId} className={estilos.item}>
                <Dados item={item} />
                <Etiqueta tom={STATUS_DA_TRIAGEM[item.status].tom}>{STATUS_DA_TRIAGEM[item.status].texto}</Etiqueta>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function Dados({ item }: { item: ItemDaFila }) {
  return (
    <div className={estilos.dados}>
      <strong>{item.nomePaciente ?? "Paciente"}</strong>
      <span>Chegada {formatarInstante(item.chegadaEm)}</span>
    </div>
  );
}
