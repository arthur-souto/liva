import type { NextRequest } from "next/server";
import { abrirSessao } from "@/shared/sessao/servidor";

export const dynamic = "force-dynamic";

export function POST(request: NextRequest): Promise<Response> {
  return abrirSessao(request, "/api/auth/link");
}
