"use client";

import { KeyRound, Send } from "lucide-react";
import { type FormEvent, useState } from "react";
import { FalhaApi } from "@/shared/api/erro";
import { cpfCompleto, mascararCpf } from "@/shared/lib/cpf";
import { apenasDigitos } from "@/shared/lib/telefone";
import { ativarCuidador, solicitarCodigoDoCuidador } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Campo } from "@/shared/ui/Campo";
import estilos from "./FormularioEntrar.module.css";
import { MolduraDeAcesso } from "./MolduraDeAcesso";

const SENHA_FORTE = /^(?=.*\p{L})(?=.*\d).{8,72}$/u;

function mensagem(erro: unknown): string {
  return erro instanceof FalhaApi ? erro.message : "Não foi possível concluir. Tente novamente.";
}

export function AtivarCuidador() {
  const [etapa, setEtapa] = useState<"identificacao" | "codigo">("identificacao");
  const [cpfDoCuidador, setCpfDoCuidador] = useState("");
  const [cpfDoPaciente, setCpfDoPaciente] = useState("");
  const [codigo, setCodigo] = useState("");
  const [senha, setSenha] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  async function solicitar(evento: FormEvent) {
    evento.preventDefault();
    if (!cpfCompleto(cpfDoCuidador) || !cpfCompleto(cpfDoPaciente)) {
      setFalha("Informe o seu CPF e o CPF da pessoa que você cuida.");
      return;
    }
    setEnviando(true);
    setFalha(null);
    try {
      const resposta = await solicitarCodigoDoCuidador(apenasDigitos(cpfDoCuidador), apenasDigitos(cpfDoPaciente));
      setAviso(resposta.mensagem);
      setEtapa("codigo");
    } catch (erro) {
      setFalha(mensagem(erro));
    } finally {
      setEnviando(false);
    }
  }

  async function ativar(evento: FormEvent) {
    evento.preventDefault();
    const problema = !/^\d{6}$/.test(codigo.trim())
      ? "O código tem 6 números."
      : !SENHA_FORTE.test(senha)
        ? "A senha deve ter de 8 a 72 caracteres, com letras e números."
        : null;
    if (problema) {
      setFalha(problema);
      return;
    }
    setEnviando(true);
    setFalha(null);
    try {
      await ativarCuidador(apenasDigitos(cpfDoCuidador), apenasDigitos(cpfDoPaciente), codigo.trim(), senha);
      window.location.assign("/portal");
    } catch (erro) {
      setFalha(mensagem(erro));
      setEnviando(false);
    }
  }

  return (
    <MolduraDeAcesso>
      {etapa === "identificacao" ? (
        <form onSubmit={solicitar} className={estilos.formulario} noValidate>
          <div>
            <h1 className={estilos.titulo}>Acesso da pessoa de confiança</h1>
            <p className={estilos.subtitulo}>Enviaremos um código para o seu WhatsApp, registrado pela unidade de saúde.</p>
          </div>
          {falha && <Alerta tom="perigo">{falha}</Alerta>}
          <Campo
            rotulo="Seu CPF"
            value={cpfDoCuidador}
            onChange={(e) => setCpfDoCuidador(mascararCpf(e.target.value))}
            inputMode="numeric"
            autoComplete="username"
            required
          />
          <Campo
            rotulo="CPF da pessoa que você cuida"
            value={cpfDoPaciente}
            onChange={(e) => setCpfDoPaciente(mascararCpf(e.target.value))}
            inputMode="numeric"
            required
          />
          <Botao type="submit" tamanho="grande" larguraTotal carregando={enviando} icone={<Send size={20} aria-hidden="true" />}>
            Receber código
          </Botao>
          <p className={estilos.rodape}>
            Já tem senha? <a href="/cuidador/entrar">Entrar</a>
          </p>
        </form>
      ) : (
        <form onSubmit={ativar} className={estilos.formulario} noValidate>
          <div>
            <h1 className={estilos.titulo}>Criar senha</h1>
            <p className={estilos.subtitulo}>Digite o código recebido e escolha uma senha.</p>
          </div>
          {aviso && <Alerta tom="info">{aviso}</Alerta>}
          {falha && <Alerta tom="perigo">{falha}</Alerta>}
          <Campo
            rotulo="Código"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value.replace(/\D/g, "").slice(0, 6))}
            inputMode="numeric"
            autoComplete="one-time-code"
            required
          />
          <Campo
            rotulo="Nova senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="new-password"
            dica="De 8 a 72 caracteres, com letras e números."
            required
          />
          <Botao type="submit" tamanho="grande" larguraTotal carregando={enviando} icone={<KeyRound size={20} aria-hidden="true" />}>
            Criar senha e entrar
          </Botao>
          <p className={estilos.rodape}>
            Não recebeu?{" "}
            <a
              href="/cuidador/ativar"
              onClick={(e) => {
                e.preventDefault();
                setEtapa("identificacao");
                setFalha(null);
              }}
            >
              Pedir outro código
            </a>
          </p>
        </form>
      )}
    </MolduraDeAcesso>
  );
}
