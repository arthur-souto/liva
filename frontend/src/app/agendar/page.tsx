import type { Metadata } from "next";
import { FormularioAgendamento } from "@/features/agendar/FormularioAgendamento";

export const metadata: Metadata = { title: "Agendar" };

const DATA = /^\d{4}-\d{2}-\d{2}$/;

export default async function Page({ searchParams }: { searchParams: Promise<{ data?: string | string[] }> }) {
  const { data } = await searchParams;
  return <FormularioAgendamento dataInicial={typeof data === "string" && DATA.test(data) ? data : undefined} />;
}
