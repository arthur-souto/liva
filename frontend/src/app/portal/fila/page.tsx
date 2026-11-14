import type { Metadata } from "next";
import { MinhaFila } from "@/features/fila/MinhaFila";

export const metadata: Metadata = { title: "Minha fila" };

export default function Page() {
  return <MinhaFila />;
}
