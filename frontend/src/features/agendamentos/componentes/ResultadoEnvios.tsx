import { CheckCheck, CircleX } from "lucide-react";
import { formatarInstante } from "@/shared/lib/datas";
import { classes } from "@/shared/lib/classes";
import { formatarTelefone } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import type { ResultadoDisparo } from "../tipos";
import estilos from "./ResultadoEnvios.module.css";

type Props = {
  titulo: string;
  resultado: ResultadoDisparo;
};

export function ResultadoEnvios({ titulo, resultado }: Props) {
  return (
    <section className={estilos.bloco} aria-label={titulo}>
      <h3 className={estilos.titulo}>{titulo}</h3>
      {resultado.motivoNaoEnvio ? (
        <Alerta tom="info" titulo="Nada foi enviado">
          {resultado.motivoNaoEnvio}
        </Alerta>
      ) : (
        <ul className={estilos.lista}>
          {resultado.envios.map((envio) => (
            <li key={`${envio.destino.e164}-${envio.enviadoEm}`} className={estilos.envio}>
              <span className={classes(estilos.icone, envio.sucesso ? estilos.sucesso : estilos.falha)}>
                {envio.sucesso ? <CheckCheck size={18} aria-hidden="true" /> : <CircleX size={18} aria-hidden="true" />}
              </span>
              <div className={estilos.corpo}>
                <p className={estilos.destino}>
                  {envio.sucesso ? "Enviado para" : "Falhou para"} {formatarTelefone(envio.destino.e164)}
                </p>
                <p className={estilos.meta}>{envio.sucesso ? formatarInstante(envio.enviadoEm) : envio.erro}</p>
                <details className={estilos.detalhes}>
                  <summary>Ver mensagem</summary>
                  <p className={estilos.texto}>{envio.texto}</p>
                </details>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
