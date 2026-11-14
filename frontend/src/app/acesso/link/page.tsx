import type { Metadata } from "next";
import { EntrarPorLink } from "@/features/acesso/EntrarPorLink";

export const metadata: Metadata = { title: "Entrando no portal" };

export default async function Page({ searchParams }: { searchParams: Promise<{ t?: string }> }) {
  const { t } = await searchParams;
  return <EntrarPorLink token={t ?? ""} />;
}
