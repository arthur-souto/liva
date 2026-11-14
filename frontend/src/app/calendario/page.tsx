import type { Metadata } from "next";
import { CalendarioMensal } from "@/features/calendario/CalendarioMensal";

export const metadata: Metadata = { title: "Calendário" };

export default function Page() {
  return <CalendarioMensal />;
}
