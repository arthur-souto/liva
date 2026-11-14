import type { Metadata } from "next";
import { AtendimentoEspecializado } from "@/features/especialista/AtendimentoEspecializado";

export const metadata: Metadata = { title: "Atendimento especializado" };

export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ triagemId: string }>;
  searchParams: Promise<{ consulta?: string }>;
}) {
  const { triagemId } = await params;
  const { consulta } = await searchParams;
  return <AtendimentoEspecializado triagemId={triagemId} consultaId={typeof consulta === "string" ? consulta : null} />;
}
