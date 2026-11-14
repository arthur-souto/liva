import { ChevronLeft, ChevronRight } from "lucide-react";
import { Botao } from "./Botao";
import estilos from "./Paginacao.module.css";

type Props = {
  pagina: number;
  totalPaginas: number;
  tamanho: number;
  total: number;
  irPara: (pagina: number) => void;
  item?: [singular: string, plural: string];
  carregando?: boolean;
};

export function Paginacao({ pagina, totalPaginas, tamanho, total, irPara, item = ["item", "itens"], carregando }: Props) {
  if (total === 0) return null;
  const nome = total === 1 ? item[0] : item[1];
  const inicio = (pagina - 1) * tamanho + 1;
  const fim = Math.min(pagina * tamanho, total);

  function ir(destino: number) {
    irPara(destino);
    window.scrollTo({ top: 0 });
  }

  return (
    <nav className={estilos.paginacao} aria-label="Paginação">
      <p className={estilos.resumo} aria-live="polite">
        {totalPaginas > 1 ? `Mostrando ${inicio}–${fim} de ${total} ${nome}` : `${total} ${nome}`}
      </p>
      {totalPaginas > 1 && (
        <div className={estilos.controles}>
          <Botao
            variante="contorno"
            tamanho="pequeno"
            icone={<ChevronLeft size={16} aria-hidden="true" />}
            disabled={pagina === 1 || carregando}
            onClick={() => ir(pagina - 1)}
          >
            Anterior
          </Botao>
          <span className={estilos.pagina}>
            Página {pagina} de {totalPaginas}
          </span>
          <Botao
            variante="contorno"
            tamanho="pequeno"
            iconeDireita={<ChevronRight size={16} aria-hidden="true" />}
            disabled={pagina === totalPaginas || carregando}
            onClick={() => ir(pagina + 1)}
          >
            Próxima
          </Botao>
        </div>
      )}
    </nav>
  );
}
