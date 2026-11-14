import type { Metadata } from "next";
import { PainelInicio } from "@/features/painel/PainelInicio";

export const metadata: Metadata = { title: "Início" };

export default function Page() {
  return <PainelInicio />;
}
