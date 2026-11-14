import type { Metadata } from "next";
import { SimuladorConversa } from "@/features/conversas/SimuladorConversa";

export const metadata: Metadata = { title: "Conversas" };

export default async function Page({ searchParams }: { searchParams: Promise<{ telefone?: string | string[] }> }) {
  const { telefone } = await searchParams;
  return <SimuladorConversa telefoneInicial={typeof telefone === "string" ? telefone.replace(/^55(?=\d{10,11}$)/, "") : undefined} />;
}
