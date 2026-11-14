import type { Metadata } from "next";
import { FilaDeTriagem } from "@/features/triagem/FilaDeTriagem";

export const metadata: Metadata = { title: "Triagem" };

export default function Page() {
  return <FilaDeTriagem />;
}
