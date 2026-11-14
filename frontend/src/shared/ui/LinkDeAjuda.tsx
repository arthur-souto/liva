import { CircleHelp } from "lucide-react";
import { BotaoLink } from "./Botao";

export function LinkDeAjuda({ topico }: { topico: string }) {
  return (
    <BotaoLink href={`/ajuda#${topico}`} variante="fantasma" tamanho="pequeno" icone={<CircleHelp size={16} aria-hidden="true" />}>
      Como funciona
    </BotaoLink>
  );
}
