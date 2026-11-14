import type { Metadata } from "next";
import { PainelAgendamentos } from "@/features/agendamentos/PainelAgendamentos";

export const metadata: Metadata = { title: "Agendamentos" };

export default function Page() {
  return <PainelAgendamentos />;
}
