"use client";

import { LogIn } from "lucide-react";
import { type FormEvent, useState } from "react";
import { FalhaApi } from "@/shared/api/erro";
import { cpfCompleto, mascararCpf } from "@/shared/lib/cpf";
import { apenasDigitos } from "@/shared/lib/telefone";
import { entrarComoCuidador } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Campo } from "@/shared/ui/Campo";
import estilos from "./FormularioEntrar.module.css";
import { MolduraDeAcesso } from "./MolduraDeAcesso";

export function EntrarComoCuidador() {
  const [cpfDoCuidador, setCpfDoCuidador] = useState("");
  const [cpfDoPaciente, setCpfDoPaciente] = useState("");
  const [senha, setSenha] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [falha, setFalha] = useState<string | null>(null);

  async function entrar(evento: FormEvent) {
    evento.preventDefault();
    if (!cpfCompleto(cpfDoCuidador) || !cpfCompleto(cpfDoPaciente) || !senha) {
      setFalha("Informe o seu CPF, o CPF da pessoa que você cuida e a senha.");
      return;
    }
    setEnviando(true);
    setFalha(null);
    try {
      await entrarComoCuidador(apenasDigitos(cpfDoCuidador), apenasDigitos(cpfDoPaciente), senha);
      window.location.assign("/portal");
    } catch (erro) {
      setFalha(erro instanceof FalhaApi ? erro.message : "Não foi possível entrar. Tente novamente.");
      setEnviando(false);
    }
  }

  return (
    <MolduraDeAcesso>
      <form onSubmit={entrar} className={estilos.formulario} noValidate>
        <div>
          <h1 className={estilos.titulo}>Pessoa de confiança</h1>
          <p className={estilos.subtitulo}>Acompanhe consultas e agende pela pessoa que você cuida.</p>
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
          Primeiro acesso ou esqueceu a senha? <a href="/cuidador/ativar">Criar senha</a>
        </p>
        <p className={estilos.rodape}>
          É paciente ou faz parte da equipe? <a href="/entrar">Voltar ao login</a>
        </p>
      </form>
    </MolduraDeAcesso>
  );
}
