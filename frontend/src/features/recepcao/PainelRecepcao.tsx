"use client";

import { ChevronRight, DoorOpen, ListOrdered, Search, UserPlus, Users, X } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useState } from "react";
import { filaDeTriagem, pesquisarPacientesPaginado, registrarChegada } from "@/features/assistencial/api";
import { STATUS_DA_TRIAGEM } from "@/features/assistencial/rotulos";
import type { Paciente } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { mascararCpf } from "@/shared/lib/cpf";
import { formatarData, formatarInstante } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { PessoaComLupa } from "@/shared/ui/Ilustracoes";
import { Paginacao } from "@/shared/ui/Paginacao";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import { Segmentado } from "@/shared/ui/Segmentado";
import { LinkDeAjuda } from "@/shared/ui/LinkDeAjuda";
import estilos from "./PainelRecepcao.module.css";

type Aba = "pacientes" | "fila";

const POR_PAGINA = 10;

export function PainelRecepcao({ triagemRecente }: { triagemRecente: string | null }) {
  const [aba, setAba] = useState<Aba>(triagemRecente ? "fila" : "pacientes");
  const [termo, setTermo] = useState("");
  const [busca, setBusca] = useState({ termo: "", pagina: 1 });
  const [aviso, setAviso] = useState<string | null>(null);
  const fila = useConsulta(filaDeTriagem, "fila", 15000);
  const lista = useConsulta(
    () => pesquisarPacientesPaginado(busca.termo, busca.pagina, POR_PAGINA),
    `pacientes|${busca.termo}|${busca.pagina}`,
  );
  const acao = useAcao();
  const buscando = busca.termo !== "";

  function pesquisar(evento: FormEvent) {
    evento.preventDefault();
    setAviso(null);
    setBusca({ termo: termo.trim(), pagina: 1 });
  }

  function limparBusca() {
    setTermo("");
    setAviso(null);
    setBusca({ termo: "", pagina: 1 });
  }

  async function chegada(paciente: Paciente) {
    setAviso(null);
    const triagem = await acao.executar(`chegada-${paciente.id}`, () => registrarChegada(paciente.id));
    if (triagem) {
      setAviso(`${paciente.nomeSocial ?? paciente.nomeCompleto} entrou na fila de triagem.`);
      fila.recarregar();
      lista.recarregar();
    }
  }

  const naFila = new Set((fila.dados ?? []).map((item) => item.pacienteId));
  const recente = fila.dados?.find((item) => item.triagemId === triagemRecente);
  const pacientes = lista.dados?.itens;

  return (
    <div className={estilos.painel}>
      <CabecalhoPagina
        titulo="Recepção"
        subtitulo="Encontre o paciente, cadastre se for a primeira vez e registre a chegada."
        acoes={
          <>
            <LinkDeAjuda topico="buscar-paciente" />
            <BotaoLink href="/recepcao/novo" icone={<UserPlus size={18} aria-hidden="true" />}>
              Novo paciente
            </BotaoLink>
          </>
        }
      />

      <Segmentado
        rotulo="Visão da recepção"
        valor={aba}
        aoMudar={setAba}
        opcoes={[
          { valor: "pacientes", rotulo: "Pacientes", icone: <Users size={16} aria-hidden="true" /> },
          {
            valor: "fila",
            rotulo: fila.dados ? `Fila da triagem (${fila.dados.length})` : "Fila da triagem",
            icone: <ListOrdered size={16} aria-hidden="true" />,
          },
        ]}
      />

      {aba === "pacientes" && (
        <Secao
          titulo={buscando ? `Resultado da busca${lista.dados ? ` (${lista.dados.total})` : ""}` : "Pacientes"}
          descricao={buscando ? undefined : "Últimos cadastrados. Clique no paciente para abrir a ficha."}
        >
          <form onSubmit={pesquisar} className={estilos.busca} role="search">
            <Campo
              rotulo="Nome, CPF ou Cartão SUS"
              value={termo}
              onChange={(e) => setTermo(e.target.value)}
              dica="Digite ao menos 3 letras do nome ou o documento completo."
            />
            <Botao
              type="submit"
              icone={<Search size={18} aria-hidden="true" />}
              carregando={buscando && lista.carregando}
              disabled={termo.trim().length < 3}
            >
              Buscar
            </Botao>
            {buscando && (
              <Botao variante="fantasma" icone={<X size={18} aria-hidden="true" />} onClick={limparBusca}>
                Limpar
              </Botao>
            )}
          </form>

          {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
          {aviso && <Alerta tom="sucesso">{aviso}</Alerta>}

          {lista.erro ? <Alerta tom="perigo">{mensagemDeErro(lista.erro)}</Alerta> : null}
          {!pacientes && !lista.erro && <Carregando linhas={4} />}
          {!buscando && pacientes && pacientes.length === 0 && (
            <EstadoVazio
              icone={<Users size={28} aria-hidden="true" />}
              titulo="Nenhum paciente cadastrado"
              acao={
                <BotaoLink href="/recepcao/novo" variante="contorno" tamanho="pequeno">
                  Cadastrar paciente
                </BotaoLink>
              }
            />
          )}

          {buscando && pacientes && pacientes.length === 0 && (
            <EstadoVazio
              ilustracao={<PessoaComLupa />}
              icone={<Users size={28} aria-hidden="true" />}
              titulo="Nenhum paciente encontrado"
              acao={
                <BotaoLink href="/recepcao/novo" variante="contorno" tamanho="pequeno">
                  Cadastrar novo paciente
                </BotaoLink>
              }
            />
          )}

          {pacientes && pacientes.length > 0 && (
            <ul className={estilos.lista}>
              {pacientes.map((p) => (
                <li key={p.id} className={estilos.paciente}>
                  <Link href={`/recepcao/pacientes/${p.id}`} className={estilos.dados}>
                    <strong>{p.nomeSocial ?? p.nomeCompleto}</strong>
                    <span>
                      {p.cpf ? `CPF ${mascararCpf(p.cpf)}` : `CNS ${p.cartaoSus}`}
                      {p.dataNascimento && ` · nasc. ${formatarData(p.dataNascimento)}`}
                    </span>
                    <ChevronRight size={18} aria-hidden="true" className={estilos.seta} />
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
          {lista.dados && (
            <Paginacao
              {...lista.dados}
              irPara={(pagina) => setBusca((atual) => ({ ...atual, pagina }))}
              carregando={lista.carregando}
              item={["paciente", "pacientes"]}
            />
          )}
        </Secao>
      )}

      {aba === "fila" && (
        <Secao titulo="Fila da triagem" descricao="Atualiza a cada 15 segundos.">
          {recente && <Alerta tom="sucesso">{`${recente.nomePaciente ?? "Paciente"} foi cadastrado e a chegada foi registrada.`}</Alerta>}
          {fila.erro ? <Alerta tom="perigo">{mensagemDeErro(fila.erro)}</Alerta> : null}
          {fila.carregando && !fila.dados && <Carregando />}
          {fila.dados && fila.dados.length === 0 && (
            <EstadoVazio icone={<Users size={28} aria-hidden="true" />} titulo="Ninguém aguardando" />
          )}
          {fila.dados && fila.dados.length > 0 && (
            <ol className={estilos.lista}>
              {fila.dados.map((item, posicao) => (
                <li key={item.triagemId} className={classes(estilos.itemFila, item.triagemId === triagemRecente && estilos.recente)}>
                  <span className={estilos.posicao}>{posicao + 1}</span>
                  <Link href={`/recepcao/pacientes/${item.pacienteId}`} className={estilos.dados}>
                    <strong>{item.nomePaciente ?? "Paciente"}</strong>
                    <span>Chegada {formatarInstante(item.chegadaEm)}</span>
                  </Link>
                  <Etiqueta tom={STATUS_DA_TRIAGEM[item.status].tom}>{STATUS_DA_TRIAGEM[item.status].texto}</Etiqueta>
                </li>
              ))}
            </ol>
          )}
        </Secao>
      )}
    </div>
  );
}
