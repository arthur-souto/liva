import type { Metadata } from "next";
import { FormularioPaciente } from "@/features/recepcao/FormularioPaciente";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";

export const metadata: Metadata = { title: "Novo paciente" };

export default function Page() {
  return (
    <>
      <CabecalhoPagina titulo="Novo paciente" subtitulo="Antes de cadastrar, confira na busca se o paciente já existe." />
      <FormularioPaciente />
    </>
  );
}
