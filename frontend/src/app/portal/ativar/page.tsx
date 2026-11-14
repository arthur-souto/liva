import type { Metadata } from "next";
import { AtivarPortal } from "@/features/acesso/AtivarPortal";

export const metadata: Metadata = { title: "Ativar portal do paciente" };

export default function Page() {
  return <AtivarPortal />;
}
