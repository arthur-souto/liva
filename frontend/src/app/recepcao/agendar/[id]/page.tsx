import type { Metadata } from "next";
import { AgendarPelaRecepcao } from "@/features/recepcao/AgendarPelaRecepcao";

export const metadata: Metadata = { title: "Agendar consulta" };

export default async function Page({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ paciente?: string }> }) {
  const { id } = await params;
  const { paciente } = await searchParams;
  return <AgendarPelaRecepcao encaminhamentoId={id} pacienteId={typeof paciente === "string" ? paciente : null} />;
}
