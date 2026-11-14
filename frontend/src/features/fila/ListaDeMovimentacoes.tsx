import { formatarInstante } from "@/shared/lib/datas";
import { ROTULOS_DE_PAPEL, type Papel } from "@/shared/sessao/sessao";
import { EVENTOS } from "./rotulos";
import type { MovimentacaoDaFila } from "./tipos";
import estilos from "./PainelDaFila.module.css";

function quem(papel: string | null): string {
  if (!papel || papel === "SISTEMA") return "Automático";
  return ROTULOS_DE_PAPEL[papel as Papel] ?? papel;
}

export function ListaDeMovimentacoes({ itens, comPaciente = true }: { itens: MovimentacaoDaFila[]; comPaciente?: boolean }) {
  return (
    <ol className={estilos.linhaDoTempo}>
      {itens.map((m) => (
        <li key={m.id}>
          <span className={estilos.ponto} aria-hidden="true" />
          <div>
            <p className={estilos.evento}>
              <strong>{EVENTOS[m.evento]}</strong>
              {comPaciente && m.paciente && <span> · {m.paciente}</span>}
            </p>
            {m.detalhe && <p className={estilos.detalhe}>{m.detalhe}</p>}
            <p className={estilos.quando}>
              {formatarInstante(m.registradaEm)} · {quem(m.papel)}
            </p>
          </div>
        </li>
      ))}
    </ol>
  );
}
