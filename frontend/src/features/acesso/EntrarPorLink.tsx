"use client";

import { LogIn } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { FalhaApi } from "@/shared/api/erro";
import { entrarPorLink } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { BotaoLink } from "@/shared/ui/Botao";
import { Carregando } from "@/shared/ui/Carregando";
import estilos from "./FormularioEntrar.module.css";
import { MolduraDeAcesso } from "./MolduraDeAcesso";

function destinoSeguro(destino: string | undefined): string {
  return destino && destino.startsWith("/portal") && !destino.startsWith("//") ? destino : "/portal";
}

export function EntrarPorLink({ token }: { token: string }) {
  const [falha, setFalha] = useState<string | null>(token ? null : "Este link está incompleto. Peça um novo pelo WhatsApp enviando MENU.");
  const iniciado = useRef(false);

  useEffect(() => {
    if (!token || iniciado.current) return;
    iniciado.current = true;
    entrarPorLink(token)
      .then((resposta) => window.location.replace(destinoSeguro(resposta.destino)))
      .catch((erro: unknown) => setFalha(erro instanceof FalhaApi ? erro.message : "Não foi possível abrir o link. Tente novamente."));
  }, [token]);

  return (
    <MolduraDeAcesso>
      <div className={estilos.formulario}>
        <div>
          <h1 className={estilos.titulo}>Portal do paciente</h1>
          <p className={estilos.subtitulo}>{falha ? "Não conseguimos abrir este link." : "Abrindo o portal para você..."}</p>
        </div>
        {falha ? (
          <>
            <Alerta tom="perigo">{falha}</Alerta>
            <BotaoLink href="/entrar" larguraTotal icone={<LogIn size={18} aria-hidden="true" />}>
              Entrar com CPF e senha
            </BotaoLink>
          </>
        ) : (
          <Carregando linhas={2} />
        )}
      </div>
    </MolduraDeAcesso>
  );
}
