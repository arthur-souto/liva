import type { Metadata } from "next";
import { AgendarPeloPortal } from "@/features/portal/AgendarPeloPortal";

export const metadata: Metadata = { title: "Agendar consulta" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <AgendarPeloPortal encaminhamentoId={id} />;
}
