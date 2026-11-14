import type { Metadata } from "next";
import { DetalheAgendamento } from "@/features/agendamentos/componentes/DetalheAgendamento";

export const metadata: Metadata = { title: "Agendamento" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <DetalheAgendamento id={id} />;
}
