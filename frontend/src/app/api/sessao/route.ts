import type { NextRequest } from "next/server";
import { apiIndisponivel, chamarApi, encerrarSessao, erro, origemConfiavel, tokenDaSessao } from "@/shared/sessao/servidor";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<Response> {
  const origem = request.nextUrl.pathname;
  const token = await tokenDaSessao();
  if (!token) return erro(401, "NAO_AUTORIZADO", "Entre para continuar.", origem);
  const resposta = await chamarApi("/api/auth/eu", { headers: { accept: "application/json" }, token });
  if (!resposta) return apiIndisponivel(origem);
  if (resposta.status === 401) await encerrarSessao();
  return new Response(await resposta.arrayBuffer(), { status: resposta.status, headers: { "content-type": "application/json" } });
}

export async function DELETE(request: NextRequest): Promise<Response> {
  if (!origemConfiavel(request)) return erro(403, "ACESSO_NEGADO", "Origem não permitida", request.nextUrl.pathname);
  await encerrarSessao();
  return new Response(null, { status: 204 });
}
