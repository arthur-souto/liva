import { cookies } from "next/headers";
import type { NextRequest } from "next/server";

export const COOKIE_SESSAO = "liva_sessao";

const METODOS_DE_ESCRITA = new Set(["POST", "PUT", "PATCH", "DELETE"]);

type SessaoDaApi = { token: string; expiraEm: string; usuario: unknown; destino?: string };

export function erro(status: number, codigo: string, mensagem: string, caminho: string): Response {
  return Response.json({ timestamp: new Date().toISOString(), status, codigo, mensagem, caminho }, { status });
}

export function apiIndisponivel(caminho: string): Response {
  return erro(503, "SERVICO_INDISPONIVEL", "API Liva fora do ar. Confira se ela está rodando em LIVA_API_URL.", caminho);
}

export function origemConfiavel(request: NextRequest): boolean {
  if (!METODOS_DE_ESCRITA.has(request.method)) return true;
  const origem = request.headers.get("origin");
  if (!origem) return true;
  try {
    return new URL(origem).host === request.headers.get("host");
  } catch {
    return false;
  }
}

export async function tokenDaSessao(): Promise<string | undefined> {
  return (await cookies()).get(COOKIE_SESSAO)?.value;
}

export async function encerrarSessao(): Promise<void> {
  (await cookies()).delete(COOKIE_SESSAO);
}

export async function chamarApi(caminho: string, init: RequestInit & { token?: string }): Promise<Response | null> {
  const base = process.env.LIVA_API_URL;
  if (!base) return null;
  const headers = new Headers(init.headers);
  if (init.token) headers.set("Authorization", `Bearer ${init.token}`);
  try {
    return await fetch(new URL(caminho, base), { ...init, headers, cache: "no-store", redirect: "manual" });
  } catch {
    return null;
  }
}

export async function abrirSessao(request: NextRequest, caminhoNaApi: string): Promise<Response> {
  const origem = request.nextUrl.pathname;
  if (!origemConfiavel(request)) return erro(403, "ACESSO_NEGADO", "Origem não permitida", origem);
  const resposta = await chamarApi(caminhoNaApi, {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: await request.text(),
  });
  if (!resposta) return apiIndisponivel(origem);
  if (!resposta.ok) return new Response(await resposta.arrayBuffer(), { status: resposta.status, headers: { "content-type": "application/json" } });

  const sessao = (await resposta.json()) as SessaoDaApi;
  const expiraEm = new Date(sessao.expiraEm);
  (await cookies()).set(COOKIE_SESSAO, sessao.token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    expires: expiraEm,
  });
  return Response.json({ usuario: sessao.usuario, expiraEm: sessao.expiraEm, destino: sessao.destino });
}
