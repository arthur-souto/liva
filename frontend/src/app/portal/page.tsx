import type { Metadata } from "next";
import { MeuPortal } from "@/features/portal/MeuPortal";

export const metadata: Metadata = { title: "Meus atendimentos" };

export default function Page() {
  return <MeuPortal />;
}
