"use client";

import { LogIn } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { type FormEvent, useState } from "react";
import { FalhaApi } from "@/shared/api/erro";
import { entrar, inicioDoPapel } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Campo } from "@/shared/ui/Campo";
import { Cartao } from "@/shared/ui/Cartao";
import { Logo } from "@/shared/ui/Logo";
import estilos from "./FormularioEntrar.module.css";

function destinoSeguro(volta: string | null): string | null {
  return volta && volta.startsWith("/") && !volta.startsWith("//") && !volta.startsWith("/entrar") ? volta : null;
}

export function FormularioEntrar() {
  const parametros = useSearchParams();
  const [login, setLogin] = useState("");
  const [senha, setSenha] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [falha, setFalha] = useState<string | null>(null);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (!login.trim() || !senha) {
      setFalha("Informe o login e a senha.");
      return;
    }
    setEnviando(true);
    setFalha(null);
    try {
      const { usuario } = await entrar(login.trim(), senha);
      window.location.assign(destinoSeguro(parametros.get("volta")) ?? inicioDoPapel(usuario.papel));
    } catch (erro) {
      setFalha(erro instanceof FalhaApi ? erro.message : "Não foi possível entrar. Tente novamente.");
      setEnviando(false);
    }
  }

  return (
    <div className={estilos.pagina}>
      <div className={estilos.marca}>
        <Logo tamanho="grande" />
        <p className={estilos.slogan}>Seu cuidado, mais perto.</p>
      </div>
      <Cartao espacamento="amplo" className={estilos.cartao}>
        <form onSubmit={enviar} className={estilos.formulario} noValidate>
          <div>
            <h1 className={estilos.titulo}>Entrar</h1>
            <p className={estilos.subtitulo}>Equipe: use o seu e-mail. Paciente: use o CPF ou o Cartão SUS.</p>
          </div>
          {falha && <Alerta tom="perigo">{falha}</Alerta>}
          <Campo
            rotulo="E-mail, CPF ou Cartão SUS"
            value={login}
            onChange={(e) => setLogin(e.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            required
          />
          <Campo
            rotulo="Senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
            required
          />
          <Botao type="submit" tamanho="grande" larguraTotal carregando={enviando} icone={<LogIn size={20} aria-hidden="true" />}>
            Entrar
          </Botao>
          <p className={estilos.rodape}>
            Primeiro acesso ou esqueceu a senha? <a href="/portal/ativar">Ative o portal do paciente</a>
          </p>
        </form>
      </Cartao>
    </div>
  );
}
