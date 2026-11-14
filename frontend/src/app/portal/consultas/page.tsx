import type { Metadata } from "next";
import { MinhasConsultas } from "@/features/portal/MinhasConsultas";

export const metadata: Metadata = { title: "Minhas consultas" };

export default function Page() {
  return <MinhasConsultas />;
}
