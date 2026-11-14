import type { Metadata } from "next";
import { PainelDaFila } from "@/features/fila/PainelDaFila";

export const metadata: Metadata = { title: "Filas de espera" };

export default function Page() {
  return <PainelDaFila />;
}
