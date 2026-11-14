import { MessagesSquare } from "lucide-react";
import { classes } from "@/shared/lib/classes";
import { formatarInstante } from "@/shared/lib/datas";
import { formatarTelefone } from "@/shared/lib/telefone";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { INTERPRETACAO, LEMBRETES } from "../rotulos";
import type { MensagemDoHistorico } from "../tipos";
import estilos from "./HistoricoMensagens.module.css";

function rotulo(m: MensagemDoHistorico): string {
  if (m.direcao === "RECEBIDA") return m.resposta ? INTERPRETACAO[m.resposta] : "Resposta não reconhecida";
  return m.tipo ? LEMBRETES[m.tipo].rotulo : "Mensagem da Liva";
}

export function HistoricoMensagens({ mensagens }: { mensagens: MensagemDoHistorico[] }) {
  if (mensagens.length === 0)
    return <EstadoVazio icone={<MessagesSquare size={28} />} titulo="Nenhuma mensagem ainda" descricao="As mensagens enviadas e as respostas aparecem aqui." />;

  return (
    <ol className={estilos.lista}>
      {mensagens.map((m, i) => (
        <li
          key={`${m.direcao}-${m.em}-${i}`}
          className={classes(estilos.balao, m.direcao === "RECEBIDA" ? estilos.recebida : estilos.enviada, !m.sucesso && estilos.falhou)}
        >
          <span className={estilos.rotulo}>{rotulo(m)}</span>
          <p className={estilos.texto}>{m.texto}</p>
          <span className={estilos.meta}>
            {m.direcao === "ENVIADA" && `Para ${formatarTelefone(m.telefone)} · `}
            {m.sucesso ? formatarInstante(m.em) : `Falhou: ${m.erro ?? "erro desconhecido"}`}
          </span>
        </li>
      ))}
    </ol>
  );
}
