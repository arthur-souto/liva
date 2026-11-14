"use client";

import { ShieldCheck, UserMinus, UserPlus } from "lucide-react";
import { type FormEvent, useState } from "react";
import { pessoasDeConfianca, revogarPessoaDeConfianca, vincularPessoaDeConfianca } from "@/features/assistencial/api";
import { PARENTESCOS } from "@/features/assistencial/rotulos";
import type { Parentesco } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { cpfCompleto, mascararCpf } from "@/shared/lib/cpf";
import { formatarData } from "@/shared/lib/datas";
import { apenasDigitos, formatarTelefone, mascararTelefone, telefoneValido } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Campo, Selecao } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { PessoasDeMaosDadas } from "@/shared/ui/Ilustracoes";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./PessoasDeConfianca.module.css";

const VAZIO = { nome: "", cpf: "", telefone: "", parentesco: "FILHO" as Parentesco, autorizacaoRegistrada: false };

export function PessoasDeConfianca({ pacienteId, cpfDoPaciente }: { pacienteId: string; cpfDoPaciente: string | null }) {
  const lista = useConsulta(() => pessoasDeConfianca(pacienteId), `pessoas-de-confianca-${pacienteId}`);
  const acao = useAcao();
  const [aberto, setAberto] = useState(false);
  const [dados, setDados] = useState(VAZIO);
  const [problema, setProblema] = useState<string | null>(null);

  async function registrar(evento: FormEvent) {
    evento.preventDefault();
    const erro =
      dados.nome.trim().length < 3
        ? "Informe o nome completo."
        : !cpfCompleto(dados.cpf)
          ? "Informe o CPF da pessoa de confiança."
          : cpfDoPaciente && apenasDigitos(dados.cpf) === cpfDoPaciente
            ? "A pessoa de confiança precisa ter um CPF diferente do paciente."
            : !telefoneValido(dados.telefone)
              ? "Informe o WhatsApp com DDD."
              : !dados.autorizacaoRegistrada
                ? "Confirme que o paciente ou responsável autorizou."
                : null;
    setProblema(erro);
    if (erro) return;
    const corpo = { ...dados, nome: dados.nome.trim(), cpf: apenasDigitos(dados.cpf), telefone: apenasDigitos(dados.telefone) };
    if (await acao.executar("vincular", () => vincularPessoaDeConfianca(pacienteId, corpo))) {
      setDados(VAZIO);
      setAberto(false);
      lista.recarregar();
    }
  }

  async function revogar(cuidadorId: string) {
    const revogada = await acao.executar(cuidadorId, async () => {
      await revogarPessoaDeConfianca(pacienteId, cuidadorId);
      return true;
    });
    if (revogada) lista.recarregar();
  }

  const pessoas = lista.dados ?? [];

  return (
    <Secao
      titulo="Pessoas de confiança"
      descricao="Familiares ou cuidadores autorizados a agendar pelo paciente e receber todos os avisos no WhatsApp."
      acoes={
        !aberto ? (
          <Botao variante="contorno" tamanho="pequeno" icone={<UserPlus size={16} aria-hidden="true" />} onClick={() => setAberto(true)}>
            Adicionar
          </Botao>
        ) : undefined
      }
    >
      {lista.erro ? <Alerta tom="perigo">{mensagemDeErro(lista.erro)}</Alerta> : null}
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      {lista.carregando && !lista.dados && <Carregando linhas={2} />}

      {lista.dados && pessoas.length === 0 && !aberto && (
        <EstadoVazio
          ilustracao={<PessoasDeMaosDadas />}
          icone={<ShieldCheck size={28} aria-hidden="true" />}
          titulo="Nenhuma pessoa de confiança registrada"
          descricao="Indicado para idosos ou pacientes que dependem de um familiar para marcar consultas."
        />
      )}

      {pessoas.length > 0 && (
        <ul className={estilos.lista}>
          {pessoas.map((p) => (
            <li key={p.id} className={estilos.pessoa}>
              <div className={estilos.dados}>
                <strong>{p.nome}</strong>
                <span>
                  CPF {mascararCpf(p.cpf)} · WhatsApp {formatarTelefone(p.telefone)} · desde {formatarData(p.autorizadoEm)}
                </span>
              </div>
              <Etiqueta tom="info">{PARENTESCOS[p.parentesco]}</Etiqueta>
              <Botao
                variante="fantasma"
                tamanho="pequeno"
                icone={<UserMinus size={16} aria-hidden="true" />}
                carregando={acao.executando === p.id}
                onClick={() => revogar(p.id)}
              >
                Revogar
              </Botao>
            </li>
          ))}
        </ul>
      )}

      {aberto && (
        <form onSubmit={registrar} className={estilos.formulario} noValidate>
          {problema && <Alerta tom="alerta">{problema}</Alerta>}
          <div className={estilos.campos}>
            <Campo rotulo="Nome completo" value={dados.nome} onChange={(e) => setDados((a) => ({ ...a, nome: e.target.value }))} required />
            <Campo
              rotulo="CPF"
              inputMode="numeric"
              value={dados.cpf}
              onChange={(e) => setDados((a) => ({ ...a, cpf: mascararCpf(e.target.value) }))}
              required
            />
            <Campo
              rotulo="WhatsApp"
              inputMode="tel"
              value={dados.telefone}
              onChange={(e) => setDados((a) => ({ ...a, telefone: mascararTelefone(e.target.value) }))}
              required
            />
            <Selecao
              rotulo="Relação com o paciente"
              value={dados.parentesco}
              onChange={(e) => setDados((a) => ({ ...a, parentesco: e.target.value as Parentesco }))}
            >
              {Object.entries(PARENTESCOS).map(([valor, texto]) => (
                <option key={valor} value={valor}>
                  {texto}
                </option>
              ))}
            </Selecao>
          </div>
          <label className={estilos.autorizacao}>
            <input
              type="checkbox"
              checked={dados.autorizacaoRegistrada}
              onChange={(e) => setDados((a) => ({ ...a, autorizacaoRegistrada: e.target.checked }))}
            />
            O paciente (ou responsável legal) autorizou esta pessoa a receber as comunicações, agendar e acompanhar os atendimentos (LGPD).
          </label>
          <div className={estilos.acoes}>
            <Botao
              variante="fantasma"
              type="button"
              onClick={() => {
                setAberto(false);
                setProblema(null);
                setDados(VAZIO);
              }}
            >
              Cancelar
            </Botao>
            <Botao type="submit" carregando={acao.executando === "vincular"} icone={<ShieldCheck size={16} aria-hidden="true" />}>
              Registrar pessoa de confiança
            </Botao>
          </div>
        </form>
      )}
    </Secao>
  );
}
