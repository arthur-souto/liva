import type { Metadata } from "next";
import { PainelAvisos } from "@/features/avisos/PainelAvisos";

export const metadata: Metadata = { title: "Avisos" };

export default function Page() {
  return <PainelAvisos />;
}
