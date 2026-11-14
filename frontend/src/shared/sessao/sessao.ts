import { type ErroApi, FalhaApi } from "@/shared/api/erro";

export type Papel = "ADMINISTRADOR" | "RECEPCAO" | "CLINICO_GERAL" | "ESPECIALISTA" | "PACIENTE" | "CUIDADOR";

export type Usuario = {
  id: string;
  nome: string;
  papel: Papel;
  profissionalId: string | null;
  pacienteId: string | null;
};

export const ROTAS_PUBLICAS = ["/entrar", "/portal/ativar", "/acesso", "/cuidador"];

export const ROTULOS_DE_PAPEL: Record<Papel, string> = {
  ADMINISTRADOR: "Administração",
  RECEPCAO: "Recepção",
  CLINICO_GERAL: "Clínico geral",
  ESPECIALISTA: "Especialista",
  PACIENTE: "Paciente",
  CUIDADOR: "Pessoa de confiança",
};

export function rotaPublica(caminho: string): boolean {
  return ROTAS_PUBLICAS.some((rota) => caminho === rota || caminho.startsWith(`${rota}/`));
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

export function solicitarCodigoDoPortal(documento: string, dataNascimento: string): Promise<{ mensagem: string }> {
  return enviar("/api/sessao/portal/codigo", "POST", { documento, dataNascimento });
}

export function ativarPortal(documento: string, dataNascimento: string, codigo: string, senha: string): Promise<{ usuario: Usuario }> {
  return enviar("/api/sessao/portal/ativar", "POST", { documento, dataNascimento, codigo, senha });
}

export function entrarPorLink(token: string): Promise<{ usuario: Usuario; destino?: string }> {
  return enviar("/api/sessao/link", "POST", { token });
}

export function solicitarCodigoDoCuidador(cpfDoCuidador: string, cpfDoPaciente: string): Promise<{ mensagem: string }> {
  return enviar("/api/sessao/cuidador/codigo", "POST", { cpfDoCuidador, cpfDoPaciente });
}

export function ativarCuidador(cpfDoCuidador: string, cpfDoPaciente: string, codigo: string, senha: string): Promise<{ usuario: Usuario }> {
  return enviar("/api/sessao/cuidador/ativar", "POST", { cpfDoCuidador, cpfDoPaciente, codigo, senha });
}

export function entrarComoCuidador(cpfDoCuidador: string, cpfDoPaciente: string, senha: string): Promise<{ usuario: Usuario }> {
  return enviar("/api/sessao/cuidador/entrar", "POST", { cpfDoCuidador, cpfDoPaciente, senha });
}
