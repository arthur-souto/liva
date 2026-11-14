"use client";

import { DoorOpen, Search, UserPlus, Users } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useState } from "react";
import { filaDeTriagem, pesquisarPacientes, registrarChegada } from "@/features/assistencial/api";
import { STATUS_DA_TRIAGEM } from "@/features/assistencial/rotulos";
import type { Paciente } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { mascararCpf } from "@/shared/lib/cpf";
import { formatarData, formatarInstante } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./PainelRecepcao.module.css";

export function PainelRecepcao() {
  const [termo, setTermo] = useState("");
  const [resultados, setResultados] = useState<Paciente[] | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const fila = useConsulta(filaDeTriagem, "fila", 15000);
  const acao = useAcao();

  async function pesquisar(evento: FormEvent) {
    evento.preventDefault();
    setAviso(null);
    const encontrados = await acao.executar("pesquisar", () => pesquisarPacientes(termo.trim()));
    if (encontrados) setResultados(encontrados);
  }

  async function chegada(paciente: Paciente) {
    setAviso(null);
    const triagem = await acao.executar(`chegada-${paciente.id}`, () => registrarChegada(paciente.id));
    if (triagem) {
      setAviso(`${paciente.nomeSocial ?? paciente.nomeCompleto} entrou na fila de triagem.`);
      fila.recarregar();
    }
  }

  const naFila = new Set((fila.dados ?? []).map((item) => item.pacienteId));

  return (
    <div className={estilos.painel}>
      <CabecalhoPagina
        titulo="Recepção"
        subtitulo="Encontre o paciente, cadastre se for a primeira vez e registre a chegada."
        acoes={
          <BotaoLink href="/recepcao/novo" icone={<UserPlus size={18} aria-hidden="true" />}>
            Novo paciente
          </BotaoLink>
        }
      />

      <div className={estilos.colunas}>
        <Secao titulo="Buscar paciente">
          <form onSubmit={pesquisar} className={estilos.busca} role="search">
            <Campo
              rotulo="Nome, CPF ou Cartão SUS"
              value={termo}
              onChange={(e) => setTermo(e.target.value)}
              dica="Digite ao menos 3 letras do nome ou o documento completo."
            />
            <Botao type="submit" icone={<Search size={18} aria-hidden="true" />} carregando={acao.executando === "pesquisar"} disabled={termo.trim().length < 3}>
              Buscar
            </Botao>
          </form>

          {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
          {aviso && <Alerta tom="sucesso">{aviso}</Alerta>}

          {resultados && resultados.length === 0 && (
            <EstadoVazio
              icone={<Users size={28} aria-hidden="true" />}
              titulo="Nenhum paciente encontrado"
              acao={
                <BotaoLink href="/recepcao/novo" variante="contorno" tamanho="pequeno">
                  Cadastrar novo paciente
                </BotaoLink>
              }
            />
          )}

          {resultados && resultados.length > 0 && (
            <ul className={estilos.lista}>
              {resultados.map((p) => (
                <li key={p.id} className={estilos.paciente}>
                  <Link href={`/recepcao/pacientes/${p.id}`} className={estilos.dados}>
                    <strong>{p.nomeSocial ?? p.nomeCompleto}</strong>
                    <span>
                      {p.cpf ? `CPF ${mascararCpf(p.cpf)}` : `CNS ${p.cartaoSus}`}
                      {p.dataNascimento && ` · nasc. ${formatarData(p.dataNascimento)}`}
                    </span>
                  </Link>
                  {naFila.has(p.id) ? (
                    <Etiqueta tom="info">Na fila</Etiqueta>
                  ) : (
                    <Botao
                      tamanho="pequeno"
                      variante="contorno"
                      icone={<DoorOpen size={16} aria-hidden="true" />}
                      carregando={acao.executando === `chegada-${p.id}`}
                      onClick={() => chegada(p)}
                    >
                      Registrar chegada
                    </Botao>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Secao>

        <Secao titulo="Fila da triagem" descricao="Atualiza a cada 15 segundos.">
          {fila.erro ? <Alerta tom="perigo">{mensagemDeErro(fila.erro)}</Alerta> : null}
          {fila.carregando && !fila.dados && <Carregando />}
          {fila.dados && fila.dados.length === 0 && <EstadoVazio icone={<Users size={28} aria-hidden="true" />} titulo="Ninguém aguardando" />}
          {fila.dados && fila.dados.length > 0 && (
            <ol className={estilos.lista}>
              {fila.dados.map((item, posicao) => (
                <li key={item.triagemId} className={estilos.itemFila}>
                  <span className={estilos.posicao}>{posicao + 1}</span>
                  <div className={estilos.dados}>
                    <strong>{item.nomePaciente ?? "Paciente"}</strong>
                    <span>Chegada {formatarInstante(item.chegadaEm)}</span>
                  </div>
                  <Etiqueta tom={STATUS_DA_TRIAGEM[item.status].tom}>{STATUS_DA_TRIAGEM[item.status].texto}</Etiqueta>
                </li>
              ))}
            </ol>
          )}
        </Secao>
      </div>
    </div>
  );
}
