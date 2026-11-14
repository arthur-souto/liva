import type { Metadata } from "next";
import { PainelRecepcao } from "@/features/recepcao/PainelRecepcao";

export const metadata: Metadata = { title: "Recepção" };

export default async function Page({ searchParams }: { searchParams: Promise<{ chegada?: string }> }) {
  const { chegada } = await searchParams;
  return <PainelRecepcao triagemRecente={typeof chegada === "string" ? chegada : null} />;
}
