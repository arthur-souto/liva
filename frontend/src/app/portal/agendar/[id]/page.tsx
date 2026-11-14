import type { Metadata } from "next";
import { AgendarPeloPortal } from "@/features/portal/AgendarPeloPortal";

export const metadata: Metadata = { title: "Agendar consulta" };

export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ remarcar?: string; versao?: string }>;
}) {
  const { id } = await params;
  const { remarcar, versao } = await searchParams;
  const remarcacao = typeof remarcar === "string" && versao !== undefined && /^\d+$/.test(versao) ? { consultaId: remarcar, versao: Number(versao) } : null;
  return <AgendarPeloPortal encaminhamentoId={id} remarcacao={remarcacao} />;
}
