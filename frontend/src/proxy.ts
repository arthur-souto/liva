import { type NextRequest, NextResponse } from "next/server";
import { inicioDoPapel, papelValido, rotaPermitida } from "@/shared/sessao/acesso";
import type { Papel } from "@/shared/sessao/sessao";

const COOKIE_SESSAO = "liva_sessao";
const ROTAS_PUBLICAS = ["/entrar", "/portal/ativar"];

function papelDoToken(token: string | undefined): Papel | null {
  const carga = token?.split(".")[1];
  if (!carga) return null;
  try {
    const dados = JSON.parse(atob(carga.replace(/-/g, "+").replace(/_/g, "/"))) as { papel?: unknown; exp?: unknown };
    if (typeof dados.exp === "number" && dados.exp * 1000 <= Date.now()) return null;
    return papelValido(dados.papel) ? dados.papel : null;
  } catch {
    return null;
  }
}

function irPara(request: NextRequest, caminho: string): NextResponse {
  return NextResponse.redirect(new URL(caminho, request.url));
}

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const publica = ROTAS_PUBLICAS.some((rota) => pathname === rota || pathname.startsWith(`${rota}/`));
  const papel = papelDoToken(request.cookies.get(COOKIE_SESSAO)?.value);

  if (!papel) {
    if (publica) return NextResponse.next();
    const destino = new URL("/entrar", request.url);
    if (pathname !== "/") destino.searchParams.set("volta", `${pathname}${search}`);
    const resposta = NextResponse.redirect(destino);
    if (request.cookies.has(COOKIE_SESSAO)) resposta.cookies.delete(COOKIE_SESSAO);
    return resposta;
  }
  if (pathname === "/entrar") return irPara(request, inicioDoPapel(papel));
  if (!publica && !rotaPermitida(papel, pathname)) return irPara(request, inicioDoPapel(papel));
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
