import type { Metadata } from "next";
import { FichaDoPaciente } from "@/features/recepcao/FichaDoPaciente";

export const metadata: Metadata = { title: "Paciente" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <FichaDoPaciente pacienteId={id} />;
}
