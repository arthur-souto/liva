import type { NovoAgendamento, TipoAtendimento } from "@/features/agendamentos/tipos";
import { cpfCompleto } from "@/shared/lib/cpf";
import { horaAtual, hoje, montarDataHora } from "@/shared/lib/datas";
import { apenasDigitos, telefoneValido } from "@/shared/lib/telefone";

export type DadosDoFormulario = {
  tipo: TipoAtendimento;
  data: string;
  hora: string;
  unidade: string;
  endereco: string;
  preparo: string;
  nome: string;
  cpf: string;
  telefone: string;
  temCuidador: "nao" | "sim";
  nomeCuidador: string;
  telefoneCuidador: string;
};

export type Erros = Partial<Record<keyof DadosDoFormulario, string>>;

export function validarAtendimento(d: DadosDoFormulario): Erros {
  const erros: Erros = {};
  if (!d.data) erros.data = "Escolha o dia do atendimento.";
  if (!d.hora) erros.hora = "Escolha o horário.";
  else if (d.data && montarDataHora(d.data, d.hora) <= montarDataHora(hoje(), horaAtual())) erros.hora = "Escolha um horário no futuro.";
  if (!d.unidade.trim()) erros.unidade = "Informe a unidade de saúde.";
  if (!d.endereco.trim()) erros.endereco = "Informe o endereço da unidade.";
  return erros;
}

export function validarPaciente(d: DadosDoFormulario): Erros {
  const erros: Erros = {};
  if (!d.nome.trim()) erros.nome = "Informe o nome do paciente.";
  if (!telefoneValido(d.telefone)) erros.telefone = "Informe DDD + número, ex.: (51) 99999-9999.";
  if (d.cpf && !cpfCompleto(d.cpf)) erros.cpf = "O CPF precisa ter 11 dígitos.";
  if (d.temCuidador === "sim") {
    if (!d.nomeCuidador.trim()) erros.nomeCuidador = "Informe o nome do cuidador.";
    if (!telefoneValido(d.telefoneCuidador)) erros.telefoneCuidador = "Informe DDD + número do cuidador.";
  }
  return erros;
}

function opcional(valor: string): string | undefined {
  return valor.trim() ? valor.trim() : undefined;
}

export function montarPedido(d: DadosDoFormulario): NovoAgendamento {
  const comCuidador = d.temCuidador === "sim";
  return {
    nomePaciente: d.nome.trim(),
    cpf: d.cpf ? apenasDigitos(d.cpf) : undefined,
    telefone: apenasDigitos(d.telefone),
    nomeCuidador: comCuidador ? opcional(d.nomeCuidador) : undefined,
    telefoneCuidador: comCuidador ? apenasDigitos(d.telefoneCuidador) : undefined,
    dataHora: montarDataHora(d.data, d.hora),
    tipo: d.tipo,
    unidade: d.unidade.trim(),
    endereco: d.endereco.trim(),
    preparo: opcional(d.preparo),
  };
}
