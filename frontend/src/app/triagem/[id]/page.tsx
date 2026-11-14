import type { Metadata } from "next";
import { AtendimentoDeTriagem } from "@/features/triagem/AtendimentoDeTriagem";

export const metadata: Metadata = { title: "Atendimento de triagem" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <AtendimentoDeTriagem triagemId={id} />;
}
