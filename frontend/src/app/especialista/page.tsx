import type { Metadata } from "next";
import { PainelEspecialista } from "@/features/especialista/PainelEspecialista";

export const metadata: Metadata = { title: "Minha agenda" };

export default function Page() {
  return <PainelEspecialista />;
}
