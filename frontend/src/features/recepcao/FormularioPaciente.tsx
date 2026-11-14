"use client";

import { Save } from "lucide-react";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { atualizarPaciente, cadastrarPaciente } from "@/features/assistencial/api";
import type { Paciente, Sexo } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { mascararCpf } from "@/shared/lib/cpf";
import { hoje } from "@/shared/lib/datas";
import { mascararTelefone } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { AreaDeTexto, Campo, Selecao } from "@/shared/ui/Campo";
import { Secao } from "@/shared/ui/Secao";
import { type Erros, type Formulario, formularioDe, mascararCartaoSus, paraDados, validar } from "./validacao";
import estilos from "./FormularioPaciente.module.css";

const SEXOS: { valor: Sexo; rotulo: string }[] = [
  { valor: "NAO_INFORMADO", rotulo: "Não informado" },
  { valor: "FEMININO", rotulo: "Feminino" },
  { valor: "MASCULINO", rotulo: "Masculino" },
  { valor: "OUTRO", rotulo: "Outro" },
];

export function FormularioPaciente({ paciente }: { paciente?: Paciente }) {
  const router = useRouter();
  const [dados, setDados] = useState<Formulario>(() => formularioDe(paciente));
  const [erros, setErros] = useState<Erros>({});
  const acao = useAcao();

  function campo<K extends keyof Formulario>(nome: K, mascara?: (v: string) => string) {
    return {
      value: dados[nome] as string,
      erro: erros[nome],
      onChange: (e: { target: { value: string } }) => {
        const valor = mascara ? mascara(e.target.value) : e.target.value;
        setDados((anterior) => ({ ...anterior, [nome]: valor }));
        setErros((anterior) => ({ ...anterior, [nome]: undefined }));
      },
    };
  }

  async function salvar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validar(dados, hoje());
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    const salvo = await acao.executar("salvar", () =>
      paciente ? atualizarPaciente(paciente.id, paraDados(dados), paciente.versao) : cadastrarPaciente(paraDados(dados)),
    );
    if (salvo) router.push(`/recepcao/pacientes/${salvo.id}`);
  }

  return (
    <form onSubmit={salvar} className={estilos.formulario} noValidate>
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

      <Secao titulo="Identificação" descricao="CPF ou Cartão SUS é obrigatório; ambos impedem cadastro em duplicidade.">
        <div className={estilos.grade}>
          <Campo rotulo="Nome completo" {...campo("nomeCompleto")} autoComplete="off" />
          <Campo rotulo="Nome social" opcional {...campo("nomeSocial")} />
          <Campo rotulo="CPF" inputMode="numeric" {...campo("cpf", mascararCpf)} />
          <Campo rotulo="Cartão SUS" inputMode="numeric" opcional {...campo("cartaoSus", mascararCartaoSus)} />
          <Campo rotulo="Data de nascimento" type="date" max={hoje()} {...campo("dataNascimento")} />
          <Selecao rotulo="Sexo" value={dados.sexo} onChange={(e) => setDados((a) => ({ ...a, sexo: e.target.value as Sexo }))}>
            {SEXOS.map((s) => (
              <option key={s.valor} value={s.valor}>
                {s.rotulo}
              </option>
            ))}
          </Selecao>
        </div>
      </Secao>

      <Secao titulo="Contato">
        <div className={estilos.grade}>
          <Campo rotulo="Telefone (WhatsApp)" inputMode="tel" {...campo("telefone", mascararTelefone)} />
          <Campo rotulo="E-mail" type="email" opcional {...campo("email")} />
          <Campo rotulo="CEP" inputMode="numeric" opcional {...campo("cep")} />
          <Campo rotulo="Logradouro" opcional {...campo("logradouro")} />
          <Campo rotulo="Número" opcional {...campo("numero")} />
          <Campo rotulo="Bairro" opcional {...campo("bairro")} />
          <Campo rotulo="Cidade" opcional {...campo("cidade")} />
          <Campo rotulo="UF" maxLength={2} opcional {...campo("uf")} />
        </div>
      </Secao>

      <Secao titulo="Cuidador e emergência" descricao="O cuidador também recebe as mensagens do WhatsApp.">
        <div className={estilos.grade}>
          <Campo rotulo="Nome do cuidador" opcional {...campo("nomeCuidador")} />
          <Campo rotulo="Telefone do cuidador" inputMode="tel" opcional {...campo("telefoneCuidador", mascararTelefone)} />
          <Campo rotulo="Contato de emergência" opcional {...campo("contatoEmergenciaNome")} />
          <Campo rotulo="Telefone de emergência" inputMode="tel" opcional {...campo("contatoEmergenciaTelefone", mascararTelefone)} />
        </div>
        <AreaDeTexto rotulo="Observações" opcional value={dados.observacoes} onChange={(e) => setDados((a) => ({ ...a, observacoes: e.target.value }))} />
        <label className={estilos.consentimento}>
          <input type="checkbox" checked={dados.consentimento} onChange={(e) => setDados((a) => ({ ...a, consentimento: e.target.checked }))} />
          O paciente autoriza receber avisos e o código do portal pelo WhatsApp (LGPD).
        </label>
      </Secao>

      <div className={estilos.acoes}>
        <Botao variante="contorno" onClick={() => router.back()}>
          Voltar
        </Botao>
        <Botao type="submit" icone={<Save size={18} aria-hidden="true" />} carregando={acao.executando === "salvar"}>
          {paciente ? "Salvar alterações" : "Cadastrar paciente"}
        </Botao>
      </div>
    </form>
  );
}
