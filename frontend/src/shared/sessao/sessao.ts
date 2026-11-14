import { type ErroApi, FalhaApi } from "@/shared/api/erro";

export type Papel = "ADMINISTRADOR" | "RECEPCAO" | "CLINICO_GERAL" | "ESPECIALISTA" | "PACIENTE";

export type Usuario = {
  id: string;
  nome: string;
  papel: Papel;
  profissionalId: string | null;
  pacienteId: string | null;
};

export const ROTAS_PUBLICAS = ["/entrar", "/portal/ativar"];

export const ROTULOS_DE_PAPEL: Record<Papel, string> = {
  ADMINISTRADOR: "Administração",
  RECEPCAO: "Recepção",
  CLINICO_GERAL: "Clínico geral",
  ESPECIALISTA: "Especialista",
  PACIENTE: "Paciente",
};

export function rotaPublica(caminho: string): boolean {
  return ROTAS_PUBLICAS.some((rota) => caminho === rota || caminho.startsWith(`${rota}/`));
}

export function inicioDoPapel(papel: Papel): string {
  return papel === "PACIENTE" ? "/portal" : "/";
}

export function irParaEntrada(): void {
  const volta = `${window.location.pathname}${window.location.search}`;
  window.location.assign(rotaPublica(window.location.pathname) ? "/entrar" : `/entrar?volta=${encodeURIComponent(volta)}`);
}

async function enviar<T>(url: string, metodo: "GET" | "POST" | "DELETE", corpo?: unknown): Promise<T> {
  let resposta: Response;
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: corpo === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
      cache: "no-store",
    });
  } catch {
    throw new FalhaApi({ timestamp: new Date().toISOString(), status: 0, codigo: "SEM_CONEXAO", mensagem: "Não foi possível conectar.", caminho: url });
  }
  if (resposta.status === 204) return undefined as T;
  const json = (await resposta.json().catch(() => null)) as unknown;
  if (!resposta.ok) {
    const falha = json as Partial<ErroApi> | null;
    throw new FalhaApi({
      timestamp: falha?.timestamp ?? new Date().toISOString(),
      status: resposta.status,
      codigo: falha?.codigo ?? "ERRO_INTERNO",
      mensagem: falha?.mensagem ?? `Resposta inesperada (HTTP ${resposta.status}).`,
      caminho: url,
    });
  }
  return json as T;
}

export function entrar(login: string, senha: string): Promise<{ usuario: Usuario }> {
  return enviar("/api/sessao/entrar", "POST", { login, senha });
}

export function usuarioAtual(): Promise<Usuario> {
  return enviar("/api/sessao", "GET");
}

export function sair(): Promise<void> {
  return enviar("/api/sessao", "DELETE");
}
