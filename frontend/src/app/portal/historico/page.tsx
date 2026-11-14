import type { Metadata } from "next";
import { MeuHistorico } from "@/features/portal/MeuHistorico";

export const metadata: Metadata = { title: "Histórico" };

export default function Page() {
  return <MeuHistorico />;
}
