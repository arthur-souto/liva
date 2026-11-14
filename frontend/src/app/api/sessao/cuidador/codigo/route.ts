import type { NextRequest } from "next/server";
import { apiIndisponivel, chamarApi, erro, origemConfiavel } from "@/shared/sessao/servidor";

export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<Response> {
  const origem = request.nextUrl.pathname;
  if (!origemConfiavel(request)) return erro(403, "ACESSO_NEGADO", "Origem não permitida", origem);
  const resposta = await chamarApi("/api/auth/cuidador/codigo", {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: await request.text(),
  });
  if (!resposta) return apiIndisponivel(origem);
  return new Response(await resposta.arrayBuffer(), { status: resposta.status, headers: { "content-type": "application/json" } });
}
