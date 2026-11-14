"use client";

import { cancelarConsulta, meuHistorico } from "@/features/assistencial/api";
import { HistoricoDeRamos } from "@/features/assistencial/componentes/HistoricoDeRamos";
import type { Consulta } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Secao } from "@/shared/ui/Secao";

export function MeuPortal() {
  const historico = useConsulta(meuHistorico, "meu-historico", 60000);
  const acao = useAcao();
  const nome = historico.dados?.paciente.nomeSocial ?? historico.dados?.paciente.nomeCompleto;

  async function cancelar(consulta: Consulta) {
    await acao.executar(consulta.id, () => cancelarConsulta(consulta.id, consulta.versao));
    historico.recarregar();
  }

  return (
    <>
      <CabecalhoPagina titulo={nome ? `Olá, ${nome.split(" ")[0]}` : "Meus atendimentos"} subtitulo="Suas triagens, encaminhamentos e consultas." />
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      {historico.erro ? <Alerta tom="perigo">{mensagemDeErro(historico.erro)}</Alerta> : null}
      {!historico.dados && !historico.erro && <Carregando linhas={5} />}
      {historico.dados && (
        <Secao titulo="Meus atendimentos" descricao="Quando um encaminhamento estiver liberado, você mesmo pode escolher o horário.">
          <HistoricoDeRamos
            historico={historico.dados}
            linkParaAgendar={(id) => `/portal/agendar/${id}`}
            aoCancelar={cancelar}
            cancelando={acao.executando}
          />
        </Secao>
      )}
    </>
  );
}
