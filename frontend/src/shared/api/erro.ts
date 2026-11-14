export type CodigoErro =
  | "REQUISICAO_INVALIDA"
  | "NAO_AUTORIZADO"
  | "ACESSO_NEGADO"
  | "NAO_ENCONTRADO"
  | "METODO_NAO_PERMITIDO"
  | "CONFLITO"
  | "TIPO_DE_CONTEUDO_NAO_SUPORTADO"
  | "ERRO_INTERNO"
  | "FALHA_WHATSAPP"
  | "SERVICO_INDISPONIVEL"
  | "SEM_CONEXAO";

export type ErroApi = {
  timestamp: string;
  status: number;
  codigo: CodigoErro;
  mensagem: string;
  caminho: string | null;
};

export class FalhaApi extends Error {
  readonly erro: ErroApi;

  constructor(erro: ErroApi) {
    super(erro.mensagem);
    this.erro = erro;
  }

  get codigo(): CodigoErro {
    return this.erro.codigo;
  }
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof FalhaApi) {
    if (erro.codigo === "NAO_AUTORIZADO") return "Sua sessão expirou. Entre novamente.";
    return erro.message;
  }
  return "Algo deu errado. Tente novamente.";
}
