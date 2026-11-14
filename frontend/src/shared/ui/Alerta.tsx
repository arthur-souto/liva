import { CircleAlert, CircleCheck, Info, TriangleAlert } from "lucide-react";
import type { ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Alerta.module.css";

const ICONES = {
  sucesso: CircleCheck,
  alerta: TriangleAlert,
  perigo: CircleAlert,
  info: Info,
};

type Props = {
  tom: keyof typeof ICONES;
  titulo?: string;
  acao?: ReactNode;
  children?: ReactNode;
};

export function Alerta({ tom, titulo, acao, children }: Props) {
  const Icone = ICONES[tom];
  return (
    <div className={classes(estilos.caixa, estilos[tom])} role={tom === "perigo" ? "alert" : "status"}>
      <Icone size={20} aria-hidden="true" className={estilos.icone} />
      <div className={estilos.conteudo}>
        {titulo && <p className={estilos.titulo}>{titulo}</p>}
        {children && <div className={estilos.texto}>{children}</div>}
      </div>
      {acao}
    </div>
  );
}
