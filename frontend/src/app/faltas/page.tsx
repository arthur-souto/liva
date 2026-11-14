import type { Metadata } from "next";
import { PainelDeFaltas } from "@/features/faltas/PainelDeFaltas";

export const metadata: Metadata = { title: "Prevenção de faltas" };

export default function Page() {
  return <PainelDeFaltas />;
}
