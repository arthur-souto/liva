import type { NextRequest } from "next/server";

export const dynamic = "force-dynamic";

const METODOS_COM_CORPO = new Set(["POST", "PUT", "PATCH", "DELETE"]);
const HEADERS_DE_ENTRADA = ["content-type", "accept"];
const HEADERS_DE_SAIDA = ["content-type", "location"];

type Contexto = { params: Promise<{ caminho: string[] }> };

function erro(status: number, codigo: string, mensagem: string, caminho: string): Response {
  return Response.json({ timestamp: new Date().toISOString(), status, codigo, mensagem, caminho }, { status });
}

function caminhoPermitido(segmentos: string[]): boolean {
  return segmentos.length > 0 && segmentos[0] !== "webhooks" && segmentos.every((s) => s !== "." && s !== ".." && !s.includes("/"));
}

async function encaminhar(request: NextRequest, { params }: Contexto): Promise<Response> {
  const { caminho } = await params;
  const origem = request.nextUrl.pathname;
  const base = process.env.LIVA_API_URL;
  const chave = process.env.LIVA_API_KEY;

  if (!base || !chave)
    return erro(503, "SERVICO_INDISPONIVEL", "LIVA_API_URL/LIVA_API_KEY não configuradas no .env.local do front.", origem);
  if (!caminhoPermitido(caminho)) return erro(404, "NAO_ENCONTRADO", "Rota não encontrada", origem);

  const destino = new URL(`/api/${caminho.map(encodeURIComponent).join("/")}`, base);
  destino.search = request.nextUrl.search;

  const headers = new Headers({ "X-API-Key": chave });
  HEADERS_DE_ENTRADA.forEach((nome) => {
    const valor = request.headers.get(nome);
    if (valor) headers.set(nome, valor);
  });

  let resposta: Response;
  try {
    resposta = await fetch(destino, {
      method: request.method,
      headers,
      body: METODOS_COM_CORPO.has(request.method) ? await request.text() : undefined,
      cache: "no-store",
      redirect: "manual",
    });
  } catch {
    return erro(503, "SERVICO_INDISPONIVEL", "API Liva fora do ar. Confira se ela está rodando em LIVA_API_URL.", origem);
  }

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
