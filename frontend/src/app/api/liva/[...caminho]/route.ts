import type { NextRequest } from "next/server";
import { apiIndisponivel, chamarApi, encerrarSessao, erro, origemConfiavel, tokenDaSessao } from "@/shared/sessao/servidor";

export const dynamic = "force-dynamic";

const METODOS_COM_CORPO = new Set(["POST", "PUT", "PATCH", "DELETE"]);
const HEADERS_DE_ENTRADA = ["content-type", "accept", "idempotency-key"];
const HEADERS_DE_SAIDA = ["content-type", "location"];
const ROTAS_BLOQUEADAS = new Set(["webhooks", "auth"]);

type Contexto = { params: Promise<{ caminho: string[] }> };

function caminhoPermitido(segmentos: string[]): boolean {
  return segmentos.length > 0 && !ROTAS_BLOQUEADAS.has(segmentos[0]) && segmentos.every((s) => s !== "." && s !== ".." && !s.includes("/"));
}

async function encaminhar(request: NextRequest, { params }: Contexto): Promise<Response> {
  const { caminho } = await params;
  const origem = request.nextUrl.pathname;

  if (!caminhoPermitido(caminho)) return erro(404, "NAO_ENCONTRADO", "Rota não encontrada", origem);
  if (!origemConfiavel(request)) return erro(403, "ACESSO_NEGADO", "Origem não permitida", origem);
  const token = await tokenDaSessao();
  if (!token) return erro(401, "NAO_AUTORIZADO", "Entre para continuar.", origem);

  const headers = new Headers();
  HEADERS_DE_ENTRADA.forEach((nome) => {
    const valor = request.headers.get(nome);
    if (valor) headers.set(nome, valor);
  });

  const resposta = await chamarApi(`/api/${caminho.map(encodeURIComponent).join("/")}${request.nextUrl.search}`, {
    method: request.method,
    headers,
    body: METODOS_COM_CORPO.has(request.method) ? await request.text() : undefined,
    token,
  });
  if (!resposta) return apiIndisponivel(origem);
  if (resposta.status === 401) await encerrarSessao();

  const saida = new Headers();
  HEADERS_DE_SAIDA.forEach((nome) => {
    const valor = resposta.headers.get(nome);
    if (valor) saida.set(nome, nome === "location" ? valor.replace(/^\/api\//, "/api/liva/") : valor);
  });
  const corpo = resposta.status === 204 ? null : await resposta.arrayBuffer();
  return new Response(corpo, { status: resposta.status, headers: saida });
}

export const GET = encaminhar;
export const POST = encaminhar;
export const PUT = encaminhar;
export const PATCH = encaminhar;
export const DELETE = encaminhar;
