"use client";

import { buscarPaciente } from "@/features/assistencial/api";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { FormularioPaciente } from "./FormularioPaciente";

export function EditarPaciente({ pacienteId }: { pacienteId: string }) {
  const paciente = useConsulta(() => buscarPaciente(pacienteId), `editar-${pacienteId}`);
  if (paciente.erro) return <Alerta tom="perigo">{mensagemDeErro(paciente.erro)}</Alerta>;
  if (!paciente.dados) return <Carregando linhas={6} />;
  return (
    <>
      <CabecalhoPagina titulo="Editar cadastro" subtitulo={paciente.dados.nomeCompleto} />
      <FormularioPaciente paciente={paciente.dados} />
    </>
  );
}
