"use client";

import { KeyRound, Send } from "lucide-react";
import { type FormEvent, useState } from "react";
import { FalhaApi } from "@/shared/api/erro";
import { ativarPortal, solicitarCodigoDoPortal } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Campo } from "@/shared/ui/Campo";
import estilos from "./FormularioEntrar.module.css";
import { MolduraDeAcesso } from "./MolduraDeAcesso";

const SENHA_FORTE = /^(?=.*\p{L})(?=.*\d).{8,72}$/u;

function mensagem(erro: unknown): string {
  return erro instanceof FalhaApi ? erro.message : "Não foi possível concluir. Tente novamente.";
}

export function AtivarPortal() {
  const [etapa, setEtapa] = useState<"identificacao" | "codigo">("identificacao");
  const [documento, setDocumento] = useState("");
  const [nascimento, setNascimento] = useState("");
  const [codigo, setCodigo] = useState("");
  const [senha, setSenha] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  async function solicitar(evento: FormEvent) {
    evento.preventDefault();
    if (!documento.trim() || !nascimento) {
      setFalha("Informe o CPF ou Cartão SUS e a data de nascimento.");
      return;
    }
    setEnviando(true);
    setFalha(null);
    try {
      const resposta = await solicitarCodigoDoPortal(documento.trim(), nascimento);
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
      await ativarPortal(documento.trim(), nascimento, codigo.trim(), senha);
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
            <h1 className={estilos.titulo}>Portal do paciente</h1>
            <p className={estilos.subtitulo}>Enviaremos um código pelo WhatsApp cadastrado na unidade.</p>
          </div>
          {falha && <Alerta tom="perigo">{falha}</Alerta>}
          <Campo rotulo="CPF ou Cartão SUS" value={documento} onChange={(e) => setDocumento(e.target.value)} inputMode="numeric" autoComplete="username" required />
          <Campo rotulo="Data de nascimento" type="date" value={nascimento} onChange={(e) => setNascimento(e.target.value)} required />
          <Botao type="submit" tamanho="grande" larguraTotal carregando={enviando} icone={<Send size={20} aria-hidden="true" />}>
            Receber código
          </Botao>
          <p className={estilos.rodape}>
            Já tem senha? <a href="/entrar">Entrar</a>
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
            Ativar e entrar
          </Botao>
          <p className={estilos.rodape}>
            Não recebeu?{" "}
            <a
              href="/portal/ativar"
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
