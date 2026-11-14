import { type NextRequest, NextResponse } from "next/server";

const COOKIE_SESSAO = "liva_sessao";
const ROTAS_PUBLICAS = ["/entrar", "/portal/ativar"];

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const publica = ROTAS_PUBLICAS.some((rota) => pathname === rota || pathname.startsWith(`${rota}/`));
  const logado = request.cookies.has(COOKIE_SESSAO);

  if (!logado && !publica) {
    const destino = new URL("/entrar", request.url);
    if (pathname !== "/") destino.searchParams.set("volta", `${pathname}${search}`);
    return NextResponse.redirect(destino);
  }
  if (logado && pathname === "/entrar") return NextResponse.redirect(new URL("/", request.url));
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
