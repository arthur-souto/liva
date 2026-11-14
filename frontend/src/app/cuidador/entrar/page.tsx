import type { Metadata } from "next";
import { EntrarComoCuidador } from "@/features/acesso/EntrarComoCuidador";

export const metadata: Metadata = { title: "Entrar como pessoa de confiança" };

export default function Page() {
  return <EntrarComoCuidador />;
}
