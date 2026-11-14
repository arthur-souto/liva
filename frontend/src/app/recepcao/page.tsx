import type { Metadata } from "next";
import { PainelRecepcao } from "@/features/recepcao/PainelRecepcao";

export const metadata: Metadata = { title: "Recepção" };

export default function Page() {
  return <PainelRecepcao />;
}
