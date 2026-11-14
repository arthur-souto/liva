import { type ErroApi, FalhaApi } from "./erro";

const BASE = "/api/liva";

type Parametros = Record<string, string | string[] | boolean | undefined>;

type Opcoes = {
  metodo?: "GET" | "POST";
  corpo?: unknown;
  parametros?: Parametros;
};

function montarUrl(caminho: string, parametros?: Parametros): string {
  const busca = new URLSearchParams();
  Object.entries(parametros ?? {}).forEach(([nome, valor]) => {
    if (valor === undefined) return;
    const valores = Array.isArray(valor) ? valor : [String(valor)];
    valores.forEach((item) => busca.append(nome, item));
  });
  const consulta = busca.toString();
  return `${BASE}${caminho}${consulta ? `?${consulta}` : ""}`;
}

function erroLocal(status: number, codigo: ErroApi["codigo"], mensagem: string, caminho: string): FalhaApi {
  return new FalhaApi({ timestamp: new Date().toISOString(), status, codigo, mensagem, caminho });
}

async function lerErro(resposta: Response, caminho: string): Promise<FalhaApi> {
  const corpo = (await resposta.json().catch(() => null)) as Partial<ErroApi> | null;
  if (corpo?.codigo && corpo.mensagem) return new FalhaApi(corpo as ErroApi);
  return erroLocal(resposta.status, "ERRO_INTERNO", `Resposta inesperada da API (HTTP ${resposta.status}).`, caminho);
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const url = montarUrl(caminho, opcoes.parametros);
  let resposta: Response;
  try {
    resposta = await fetch(url, {
      method: opcoes.metodo ?? "GET",
      headers: opcoes.corpo === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
      cache: "no-store",
    });
  } catch {
    throw erroLocal(0, "SEM_CONEXAO", "Não foi possível conectar à API.", caminho);
  }
  if (!resposta.ok) throw await lerErro(resposta, caminho);
  return (await resposta.json()) as T;
}
