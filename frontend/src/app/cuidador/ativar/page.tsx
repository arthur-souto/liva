import type { Metadata } from "next";
import { AtivarCuidador } from "@/features/acesso/AtivarCuidador";

export const metadata: Metadata = { title: "Acesso da pessoa de confiança" };

export default function Page() {
  return <AtivarCuidador />;
}
