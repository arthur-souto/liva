"use client";

import { ArrowLeft, ArrowRight, CalendarCheck2 } from "lucide-react";
import { type FormEvent, useState } from "react";
import { agendar } from "@/features/agendamentos/api";
import { TIPOS_DE_ATENDIMENTO } from "@/features/agendamentos/rotulos";
import type { AgendamentoCriado, TipoAtendimento } from "@/features/agendamentos/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { classes } from "@/shared/lib/classes";
import { mascararCpf } from "@/shared/lib/cpf";
import { adicionarDias, diaDoMes, formatarDataCurta, horaAtual, hoje, nomeDiaCurto } from "@/shared/lib/datas";
import { mascararTelefone } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Cartao } from "@/shared/ui/Cartao";
import { Chip } from "@/shared/ui/Chip";
import { Segmentado } from "@/shared/ui/Segmentado";
import { ResultadoAgendamento } from "./ResultadoAgendamento";
import { type DadosDoFormulario, type Erros, montarPedido, validarAtendimento, validarPaciente } from "./validacao";
import estilos from "./FormularioAgendamento.module.css";

const DIAS_VISIVEIS = 7;
const HORARIOS = Array.from({ length: 22 }, (_, i) => `${String(7 + Math.floor(i / 2)).padStart(2, "0")}:${i % 2 ? "30" : "00"}`);
const OPCOES_TIPO = (Object.keys(TIPOS_DE_ATENDIMENTO) as TipoAtendimento[]).map((valor) => ({
  valor,
  rotulo: TIPOS_DE_ATENDIMENTO[valor],
}));

function dadosIniciais(data: string): DadosDoFormulario {
  return {
    tipo: "CONSULTA",
    data,
    hora: "",
    unidade: "",
    endereco: "",
    preparo: "",
    nome: "",
    cpf: "",
    telefone: "",
    temCuidador: "nao",
    nomeCuidador: "",
    telefoneCuidador: "",
  };
}

export function FormularioAgendamento({ dataInicial }: { dataInicial?: string }) {
  const [inicioDaFaixa] = useState(() => (dataInicial && dataInicial > hoje() ? dataInicial : hoje()));
  const [dados, setDados] = useState(() => dadosIniciais(inicioDaFaixa));
  const [passo, setPasso] = useState<1 | 2>(1);
  const [erros, setErros] = useState<Erros>({});
  const [enviando, setEnviando] = useState(false);
  const [falha, setFalha] = useState<string | null>(null);
  const [criado, setCriado] = useState<AgendamentoCriado | null>(null);

  const dias = Array.from({ length: DIAS_VISIVEIS }, (_, i) => adicionarDias(inicioDaFaixa, i));
  const ehHoje = dados.data === hoje();
  const agora = horaAtual();
  const horarioPersonalizado = Boolean(dados.hora) && !HORARIOS.includes(dados.hora);

  function alterar<K extends keyof DadosDoFormulario>(campo: K, valor: DadosDoFormulario[K]) {
    setDados((anterior) => ({ ...anterior, [campo]: valor }));
    setErros((anterior) => ({ ...anterior, [campo]: undefined }));
  }

  function escolherDia(data: string) {
    setDados((anterior) => ({ ...anterior, data, hora: data === hoje() && anterior.hora <= horaAtual() ? "" : anterior.hora }));
    setErros((anterior) => ({ ...anterior, data: undefined, hora: undefined }));
  }

  function continuar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validarAtendimento(dados);
    setErros(encontrados);
    if (Object.keys(encontrados).length === 0) setPasso(2);
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validarPaciente(dados);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setEnviando(true);
    setFalha(null);
    try {
      setCriado(await agendar(montarPedido(dados)));
    } catch (erro) {
      setFalha(mensagemDeErro(erro));
    } finally {
      setEnviando(false);
    }
  }

  function recomecar() {
    setDados(dadosIniciais(inicioDaFaixa));
    setErros({});
    setFalha(null);
    setCriado(null);
    setPasso(1);
  }

  if (criado) return <ResultadoAgendamento criado={criado} aoRecomecar={recomecar} />;

  const resumo = dados.hora ? `${formatarDataCurta(dados.data)}, ${dados.hora}` : formatarDataCurta(dados.data);

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo={passo === 1 ? "Novo agendamento" : "Dados do paciente"} subtitulo={`Passo ${passo} de 2`} />

      <div className={estilos.progresso} aria-hidden="true">
        <span className={classes(estilos.etapa, estilos.feita)} />
        <span className={classes(estilos.etapa, passo === 2 && estilos.feita)} />
      </div>

      {passo === 1 ? (
        <form className={estilos.formulario} onSubmit={continuar} noValidate>
          <Cartao className={estilos.bloco}>
            <h2 className={estilos.tituloBloco}>Tipo de atendimento</h2>
            <Segmentado rotulo="Tipo de atendimento" opcoes={OPCOES_TIPO} valor={dados.tipo} aoMudar={(tipo) => alterar("tipo", tipo)} />
          </Cartao>

          <Cartao className={estilos.bloco}>
            <div className={estilos.cabecalhoBloco}>
              <h2 className={estilos.tituloBloco}>Dia</h2>
              <label className={estilos.outraData}>
                <span className="somente-leitor-de-tela">Outra data</span>
                <input type="date" min={hoje()} value={dados.data} onChange={(e) => e.target.value && escolherDia(e.target.value)} />
              </label>
            </div>
            <div className={estilos.dias} role="radiogroup" aria-label="Dia do atendimento">
              {dias.map((dia) => (
                <button
                  key={dia}
                  type="button"
                  role="radio"
                  aria-checked={dia === dados.data}
                  className={classes(estilos.dia, dia === dados.data && estilos.diaAtivo)}
                  onClick={() => escolherDia(dia)}
                >
                  <span className={estilos.diaSemana}>{nomeDiaCurto(dia)}</span>
                  <span className={estilos.diaNumero}>{diaDoMes(dia)}</span>
                </button>
              ))}
            </div>
            {!dias.includes(dados.data) && <p className={estilos.dica}>Dia escolhido: {formatarDataCurta(dados.data)}</p>}
            {erros.data && <p className={estilos.erro}>{erros.data}</p>}
          </Cartao>

          <Cartao className={estilos.bloco}>
            <h2 className={estilos.tituloBloco}>Horário</h2>
            <div className={estilos.horarios}>
              {HORARIOS.map((hora) => (
                <Chip key={hora} selecionado={dados.hora === hora} disabled={ehHoje && hora <= agora} onClick={() => alterar("hora", hora)}>
                  {hora}
                </Chip>
              ))}
            </div>
            <Campo
              rotulo="Outro horário"
              type="time"
              value={horarioPersonalizado ? dados.hora : ""}
              onChange={(e) => alterar("hora", e.target.value)}
              erro={erros.hora}
              opcional
            />
          </Cartao>

          <Cartao className={estilos.bloco}>
            <h2 className={estilos.tituloBloco}>Local</h2>
            <Campo rotulo="Unidade de saúde" value={dados.unidade} onChange={(e) => alterar("unidade", e.target.value)} erro={erros.unidade} placeholder="UBS Centro" />
            <Campo rotulo="Endereço" value={dados.endereco} onChange={(e) => alterar("endereco", e.target.value)} erro={erros.endereco} placeholder="Rua das Flores, 120" />
            <Campo
              rotulo="Preparo"
              value={dados.preparo}
              onChange={(e) => alterar("preparo", e.target.value)}
              dica="Vai no lembrete da véspera."
              placeholder="Jejum de 8 horas"
              opcional
            />
          </Cartao>

          <div className={estilos.rodape}>
            <Botao type="submit" tamanho="grande" larguraTotal iconeDireita={<ArrowRight size={18} aria-hidden="true" />}>
              Continuar · {resumo}
            </Botao>
          </div>
        </form>
      ) : (
        <form className={estilos.formulario} onSubmit={enviar} noValidate>
          <Cartao variante="suave" className={estilos.resumo}>
            <CalendarCheck2 size={20} aria-hidden="true" />
            <span>
              {TIPOS_DE_ATENDIMENTO[dados.tipo]} · {resumo} · {dados.unidade}
            </span>
          </Cartao>

          <Cartao className={estilos.bloco}>
            <h2 className={estilos.tituloBloco}>Paciente</h2>
            <Campo rotulo="Nome completo" value={dados.nome} onChange={(e) => alterar("nome", e.target.value)} erro={erros.nome} autoComplete="name" />
            <Campo
              rotulo="WhatsApp"
              type="tel"
              inputMode="numeric"
              value={dados.telefone}
              onChange={(e) => alterar("telefone", mascararTelefone(e.target.value))}
              erro={erros.telefone}
              placeholder="(51) 99999-9999"
            />
            <Campo
              rotulo="CPF"
              inputMode="numeric"
              value={dados.cpf}
              onChange={(e) => alterar("cpf", mascararCpf(e.target.value))}
              erro={erros.cpf}
              dica="Com CPF, o paciente já cadastrado é reaproveitado."
              opcional
            />
          </Cartao>

          <Cartao className={estilos.bloco}>
            <h2 className={estilos.tituloBloco}>Cuidador</h2>
            <Segmentado
              rotulo="Tem cuidador"
              opcoes={[
                { valor: "nao", rotulo: "Sem cuidador" },
                { valor: "sim", rotulo: "Com cuidador" },
              ]}
              valor={dados.temCuidador}
              aoMudar={(valor) => alterar("temCuidador", valor)}
            />
            {dados.temCuidador === "sim" && (
              <>
                <Campo rotulo="Nome do cuidador" value={dados.nomeCuidador} onChange={(e) => alterar("nomeCuidador", e.target.value)} erro={erros.nomeCuidador} />
                <Campo
                  rotulo="WhatsApp do cuidador"
                  type="tel"
                  inputMode="numeric"
                  value={dados.telefoneCuidador}
                  onChange={(e) => alterar("telefoneCuidador", mascararTelefone(e.target.value))}
                  erro={erros.telefoneCuidador}
                  dica="O cuidador recebe uma cópia de cada mensagem."
                />
              </>
            )}
          </Cartao>

          {falha && (
            <Alerta tom="perigo" titulo="Não foi possível agendar">
              {falha}
            </Alerta>
          )}

          <div className={classes(estilos.rodape, estilos.rodapeDuplo)}>
            <Botao variante="contorno" tamanho="grande" icone={<ArrowLeft size={18} aria-hidden="true" />} onClick={() => setPasso(1)} disabled={enviando}>
              Voltar
            </Botao>
            <Botao type="submit" tamanho="grande" larguraTotal carregando={enviando} iconeDireita={<ArrowRight size={18} aria-hidden="true" />}>
              Agendar e avisar no WhatsApp
            </Botao>
          </div>
        </form>
      )}
    </div>
  );
}
