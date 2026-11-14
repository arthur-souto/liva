import type { DadosDoPaciente, Paciente, Sexo } from "@/features/assistencial/tipos";
import { apenasDigitos, telefoneValido } from "@/shared/lib/telefone";

export type Formulario = {
  nomeCompleto: string;
  nomeSocial: string;
  cpf: string;
  cartaoSus: string;
  dataNascimento: string;
  sexo: Sexo;
  email: string;
  telefone: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  cidade: string;
  uf: string;
  nomeCuidador: string;
  telefoneCuidador: string;
  contatoEmergenciaNome: string;
  contatoEmergenciaTelefone: string;
  observacoes: string;
  consentimento: boolean;
};

export type Erros = Partial<Record<keyof Formulario, string>>;

export function formularioDe(p?: Paciente): Formulario {
  return {
    nomeCompleto: p?.nomeCompleto ?? "",
    nomeSocial: p?.nomeSocial ?? "",
    cpf: p?.cpf ?? "",
    cartaoSus: p?.cartaoSus ?? "",
    dataNascimento: p?.dataNascimento ?? "",
    sexo: p?.sexo ?? "NAO_INFORMADO",
    email: p?.email ?? "",
    telefone: p?.telefone ?? "",
    cep: p?.endereco?.cep ?? "",
    logradouro: p?.endereco?.logradouro ?? "",
    numero: p?.endereco?.numero ?? "",
    bairro: p?.endereco?.bairro ?? "",
    cidade: p?.endereco?.cidade ?? "",
    uf: p?.endereco?.uf ?? "",
    nomeCuidador: p?.nomeCuidador ?? "",
    telefoneCuidador: p?.telefoneCuidador ?? "",
    contatoEmergenciaNome: p?.contatoEmergenciaNome ?? "",
    contatoEmergenciaTelefone: p?.contatoEmergenciaTelefone ?? "",
    observacoes: p?.observacoes ?? "",
    consentimento: p?.consentimento ?? false,
  };
}

export function validar(f: Formulario, hoje: string): Erros {
  const erros: Erros = {};
  if (f.nomeCompleto.trim().length < 3) erros.nomeCompleto = "Informe o nome completo.";
  const cpf = apenasDigitos(f.cpf);
  const cns = apenasDigitos(f.cartaoSus);
  if (!cpf && !cns) erros.cpf = "Informe o CPF ou o Cartão SUS.";
  if (cpf && cpf.length !== 11) erros.cpf = "O CPF tem 11 dígitos.";
  if (cns && cns.length !== 15) erros.cartaoSus = "O Cartão SUS tem 15 dígitos.";
  if (!f.dataNascimento) erros.dataNascimento = "Informe a data de nascimento.";
  else if (f.dataNascimento > hoje) erros.dataNascimento = "A data não pode estar no futuro.";
  if (!telefoneValido(f.telefone)) erros.telefone = "Informe DDD + número.";
  if (f.telefoneCuidador && !telefoneValido(f.telefoneCuidador)) erros.telefoneCuidador = "Informe DDD + número.";
  if (f.telefoneCuidador && !f.nomeCuidador.trim()) erros.nomeCuidador = "Informe o nome do cuidador.";
  if (f.contatoEmergenciaTelefone && !telefoneValido(f.contatoEmergenciaTelefone))
    erros.contatoEmergenciaTelefone = "Informe DDD + número.";
  return erros;
}

function opcional(valor: string): string | null {
  return valor.trim() ? valor.trim() : null;
}

export function paraDados(f: Formulario): DadosDoPaciente {
  const temEndereco = [f.cep, f.logradouro, f.numero, f.bairro, f.cidade, f.uf].some((v) => v.trim());
  return {
    nomeCompleto: f.nomeCompleto.trim(),
    nomeSocial: opcional(f.nomeSocial),
    cpf: opcional(apenasDigitos(f.cpf)),
    cartaoSus: opcional(apenasDigitos(f.cartaoSus)),
    dataNascimento: f.dataNascimento || null,
    sexo: f.sexo,
    email: opcional(f.email),
    telefone: apenasDigitos(f.telefone),
    endereco: temEndereco
      ? {
          cep: opcional(apenasDigitos(f.cep)),
          logradouro: opcional(f.logradouro),
          numero: opcional(f.numero),
          complemento: null,
          bairro: opcional(f.bairro),
          cidade: opcional(f.cidade),
          uf: opcional(f.uf.toUpperCase()),
        }
      : null,
    nomeCuidador: opcional(f.nomeCuidador),
    telefoneCuidador: opcional(apenasDigitos(f.telefoneCuidador)),
    contatoEmergenciaNome: opcional(f.contatoEmergenciaNome),
    contatoEmergenciaTelefone: opcional(apenasDigitos(f.contatoEmergenciaTelefone)),
    observacoes: opcional(f.observacoes),
    consentimento: f.consentimento,
  };
}

export function mascararCartaoSus(valor: string): string {
  const d = apenasDigitos(valor).slice(0, 15);
  return d.replace(/^(\d{3})(\d)/, "$1 $2").replace(/^(\d{3}) (\d{4})(\d)/, "$1 $2 $3").replace(/^(\d{3}) (\d{4}) (\d{4})(\d)/, "$1 $2 $3 $4");
}
