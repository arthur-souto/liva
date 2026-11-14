import estilos from "./Carregando.module.css";

export function Carregando({ linhas = 3 }: { linhas?: number }) {
  return (
    <div className={estilos.lista} role="status" aria-label="Carregando">
      {Array.from({ length: linhas }, (_, i) => (
        <span key={i} className={estilos.linha} />
      ))}
    </div>
  );
}
