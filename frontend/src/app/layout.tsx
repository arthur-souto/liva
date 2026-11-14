import type { Metadata } from "next";
import { Inter, Nunito } from "next/font/google";
import type { ReactNode } from "react";
import "@/shared/styles/token.css";
import "@/shared/styles/base.css";
import { Libras } from "@/shared/libras/Libras";
import { SCRIPT_DO_TEMA } from "@/shared/tema/tema";
import { Estrutura } from "@/shared/ui/Estrutura";

const inter = Inter({ subsets: ["latin"], variable: "--fonte-inter", display: "swap" });
const nunito = Nunito({ subsets: ["latin"], weight: ["800"], variable: "--fonte-nunito", display: "swap" });

export const metadata: Metadata = {
  title: { default: "Liva", template: "%s · Liva" },
  description: "Agendamentos e confirmações do SUS pelo WhatsApp.",
};

export default function LayoutRaiz({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR" className={`${inter.variable} ${nunito.variable}`} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: SCRIPT_DO_TEMA }} />
      </head>
      <body suppressHydrationWarning>
        <Estrutura>{children}</Estrutura>
        <Libras />
      </body>
    </html>
  );
}
