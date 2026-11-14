import type { Metadata } from "next";
import { EditarPaciente } from "@/features/recepcao/EditarPaciente";

export const metadata: Metadata = { title: "Editar paciente" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <EditarPaciente pacienteId={id} />;
}
