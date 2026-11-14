import type { Consulta } from "@/features/assistencial/tipos";
import { requisitar } from "@/shared/api/cliente";
import type { DetalheDaEntrada, MinhaFila, PainelDaFila, PedidoDoPainel, ResumoDoCiclo } from "./tipos";

const id = encodeURIComponent;

export function minhaFila(): Promise<MinhaFila> {
  return requisitar<MinhaFila>("/portal/fila");
}

export function aceitarOferta(ofertaId: string): Promise<Consulta> {
  return requisitar<Consulta>(`/portal/fila/ofertas/${id(ofertaId)}/aceitar`, { metodo: "POST" });
}

export function recusarOferta(ofertaId: string): Promise<void> {
  return requisitar<void>(`/portal/fila/ofertas/${id(ofertaId)}/recusar`, { metodo: "POST" });
}

export function painelDaFila({ especialidade, profissional, unidade, pagina, tamanho }: PedidoDoPainel): Promise<PainelDaFila> {
  return requisitar<PainelDaFila>("/filas", {
    parametros: { especialidade, profissional, unidade, pagina: String(pagina), tamanho: String(tamanho) },
  });
}

export function detalharEntrada(entradaId: string): Promise<DetalheDaEntrada> {
  return requisitar<DetalheDaEntrada>(`/filas/entradas/${id(entradaId)}`);
}

export function pausarEntrada(entradaId: string, motivo: string): Promise<void> {
  return requisitar<void>(`/filas/entradas/${id(entradaId)}/pausar`, { metodo: "POST", corpo: { motivo } });
}

export function retomarEntrada(entradaId: string): Promise<void> {
  return requisitar<void>(`/filas/entradas/${id(entradaId)}/retomar`, { metodo: "POST" });
}

export function registrarAceite(ofertaId: string): Promise<Consulta> {
  return requisitar<Consulta>(`/filas/ofertas/${id(ofertaId)}/aceitar`, { metodo: "POST" });
}

export function registrarRecusa(ofertaId: string): Promise<void> {
  return requisitar<void>(`/filas/ofertas/${id(ofertaId)}/recusar`, { metodo: "POST" });
}

export function confirmarPresenca(consultaId: string): Promise<Consulta> {
  return requisitar<Consulta>(`/filas/consultas/${id(consultaId)}/confirmar-presenca`, { metodo: "POST" });
}

export function processarFila(): Promise<ResumoDoCiclo> {
  return requisitar<ResumoDoCiclo>("/filas/processar", { metodo: "POST" });
}
