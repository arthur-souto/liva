import type { Metadata } from "next";
import { GuiaDaEquipe } from "@/features/ajuda/GuiaDaEquipe";

export const metadata: Metadata = { title: "Como usar" };

export default function Page() {
  return <GuiaDaEquipe />;
}
