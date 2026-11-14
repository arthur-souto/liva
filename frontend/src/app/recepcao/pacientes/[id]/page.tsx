import type { Metadata } from "next";
import { FichaDoPaciente } from "@/features/recepcao/FichaDoPaciente";

export const metadata: Metadata = { title: "Paciente" };

export default async function Page({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ chegada?: string }> }) {
  const { id } = await params;
  const { chegada } = await searchParams;
  return <FichaDoPaciente pacienteId={id} chegadaFalhou={chegada === "falhou"} />;
}
