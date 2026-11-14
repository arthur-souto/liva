import { type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes, useId } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Campo.module.css";

type Moldura = {
  rotulo: string;
  dica?: string;
  erro?: string;
  opcional?: boolean;
};

function Envoltorio({ id, rotulo, dica, erro, opcional, children }: Moldura & { id: string; children: ReactNode }) {
  return (
    <div className={estilos.campo}>
      <label htmlFor={id} className={estilos.rotulo}>
        {rotulo}
        {opcional && <span className={estilos.opcional}>opcional</span>}
      </label>
      {children}
      {erro ? (
        <p id={`${id}-mensagem`} className={estilos.erro} role="alert">
          {erro}
        </p>
      ) : (
        dica && (
          <p id={`${id}-mensagem`} className={estilos.dica}>
            {dica}
          </p>
        )
      )}
    </div>
  );
}

export function Campo({ rotulo, dica, erro, opcional, className, ...entrada }: Moldura & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} dica={dica} erro={erro} opcional={opcional}>
      <input
        id={id}
        {...entrada}
        className={classes(estilos.entrada, erro && estilos.invalida, className)}
        aria-invalid={Boolean(erro)}
        aria-describedby={erro || dica ? `${id}-mensagem` : undefined}
      />
    </Envoltorio>
  );
}

export function Selecao({ rotulo, dica, erro, opcional, className, children, ...selecao }: Moldura & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} dica={dica} erro={erro} opcional={opcional}>
      <select id={id} {...selecao} className={classes(estilos.entrada, erro && estilos.invalida, className)} aria-invalid={Boolean(erro)}>
        {children}
      </select>
    </Envoltorio>
  );
}

export function AreaDeTexto({ rotulo, dica, erro, opcional, className, ...area }: Moldura & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} dica={dica} erro={erro} opcional={opcional}>
      <textarea
        id={id}
        rows={3}
        {...area}
        className={classes(estilos.entrada, estilos.area, erro && estilos.invalida, className)}
        aria-invalid={Boolean(erro)}
        aria-describedby={erro || dica ? `${id}-mensagem` : undefined}
      />
    </Envoltorio>
  );
}
