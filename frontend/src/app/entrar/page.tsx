import type { Metadata } from "next";
import { Suspense } from "react";
import { FormularioEntrar } from "@/features/acesso/FormularioEntrar";

export const metadata: Metadata = { title: "Entrar" };

export default function Page() {
  return (
    <Suspense>
      <FormularioEntrar />
    </Suspense>
  );
}
