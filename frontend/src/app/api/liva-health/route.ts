export const dynamic = "force-dynamic";

export async function GET(): Promise<Response> {
  const base = process.env.LIVA_API_URL;
  if (!base) return Response.json({ status: "DOWN" });
  try {
    const resposta = await fetch(new URL("/actuator/health", base), { cache: "no-store", signal: AbortSignal.timeout(3000) });
    const corpo = (await resposta.json()) as { status?: string };
    return Response.json({ status: resposta.ok && corpo.status === "UP" ? "UP" : "DOWN" });
  } catch {
    return Response.json({ status: "DOWN" });
  }
}
