"use client";

import { ArrowRight, CalendarCheck, CalendarPlus, CalendarX, ClipboardList, Inbox } from "lucide-react";
import Link from "next/link";
import { PRIORIDADES, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { formatarInstante } from "@/shared/lib/datas";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { Banner } from "@/shared/ui/Banner";
import { BotaoLink } from "@/shared/ui/Botao";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { PessoaAcenando, PessoasDeMaosDadas } from "@/shared/ui/Ilustracoes";
import { Secao } from "@/shared/ui/Secao";
import { CartaoDeConsulta } from "./CartaoDeConsulta";
import { consultasDoHistorico, paraAgendar, separar } from "./consultas";
import { useNomesDosProfissionais, usePortal } from "./usePortal";
import estilos from "./Portal.module.css";

export function MeuPortal() {
  const historico = usePortal();
  const usuario = useConsulta(usuarioAtual, "usuario");
  const cuidando = usuario.dados?.papel === "CUIDADOR";
  const nomes = useNomesDosProfissionais(historico.dados);
  const h = historico.dados;
  const nome = h?.paciente.nomeSocial ?? h?.paciente.nomeCompleto;
  const { proximas } = h ? separar(consultasDoHistorico(h)) : { proximas: [] };
  const pendentes = h ? paraAgendar(h) : [];
  const [primeira, ...demais] = proximas;

  return (
    <div className={estilos.pagina}>
      <Banner
        titulo={cuidando ? (nome ? `Portal de ${nome}` : "Portal") : nome ? `Olá, ${nome.split(" ")[0]}` : "Meu portal"}
        ilustracao={cuidando ? <PessoasDeMaosDadas /> : <PessoaAcenando />}
      >
        {cuidando
          ? "Consultas, encaminhamentos e histórico de quem você cuida."
          : "Suas consultas, encaminhamentos e histórico de atendimento no SUS."}
      </Banner>
      {cuidando && usuario.dados && (
        <Alerta tom="info" titulo="Você está como pessoa de confiança">
          {usuario.dados.nome.split(" ")[0]}, você pode agendar, remarcar e cancelar consultas{nome ? ` de ${nome}` : ""}. Os avisos chegam
          no seu WhatsApp e no da pessoa cuidada.
        </Alerta>
      )}
      {historico.erro ? <Alerta tom="perigo">{mensagemDeErro(historico.erro)}</Alerta> : null}
      {!h && !historico.erro && <Carregando linhas={5} />}

      {h && (
        <Secao titulo="Escolha um horário" descricao="Encaminhamentos liberados para você escolher o dia e o horário.">
          {pendentes.length === 0 ? (
            <EstadoVazio
              icone={<CalendarCheck size={28} aria-hidden="true" />}
              titulo="Você não tem encaminhamentos aguardando agendamento"
              descricao="Quando a equipe da unidade liberar um encaminhamento, ele aparece aqui para você escolher o horário."
            />
          ) : (
            <ul className={estilos.lista}>
              {pendentes.map((e) => (
                <li key={e.id} className={estilos.pendente}>
                  <div className={estilos.dados}>
                    <strong>{nomeDaEspecialidade(e.especialidade)}</strong>
                    <span>Encaminhado em {formatarInstante(e.criadoEm)}</span>
                  </div>
                  <Etiqueta tom={PRIORIDADES[e.prioridade].tom}>{PRIORIDADES[e.prioridade].texto}</Etiqueta>
                  <BotaoLink href={`/portal/agendar/${e.id}`} tamanho="pequeno" icone={<CalendarPlus size={16} aria-hidden="true" />}>
                    Escolher horário
                  </BotaoLink>
                </li>
              ))}
            </ul>
          )}
        </Secao>
      )}

      {h && (
        <Secao
          titulo="Próxima consulta"
          acoes={
            proximas.length > 0 ? (
              <Link href="/portal/consultas" className={estilos.link}>
                Ver todas <ArrowRight size={16} aria-hidden="true" />
              </Link>
            ) : undefined
          }
        >
          {primeira ? (
            <CartaoDeConsulta
              item={primeira}
              nomeProfissional={primeira.consulta.profissionalId ? nomes.get(primeira.consulta.profissionalId) : undefined}
              destaque
              aoAlterar={historico.recarregar}
            />
          ) : (
            <EstadoVazio
              icone={<CalendarX size={28} aria-hidden="true" />}
              titulo="Nenhuma consulta marcada"
              descricao={
                pendentes.length > 0 ? "Escolha um horário para o seu encaminhamento acima." : "Você ainda não tem consultas agendadas."
              }
            />
          )}
          {demais.length > 0 && (
            <p className={estilos.mais}>
              Mais {demais.length} {demais.length === 1 ? "consulta marcada" : "consultas marcadas"}.
            </p>
          )}
        </Secao>
      )}

      {h && (
        <div className={estilos.atalhos}>
          <Link href="/portal/consultas" className={estilos.atalho}>
            <Inbox size={22} aria-hidden="true" />
            <span>
              <strong>Minhas consultas</strong>
              Próximas, realizadas e canceladas
            </span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
          <Link href="/portal/historico" className={estilos.atalho}>
            <ClipboardList size={22} aria-hidden="true" />
            <span>
              <strong>Histórico</strong>
              Triagens, encaminhamentos e desfechos
            </span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        </div>
      )}
    </div>
  );
}
