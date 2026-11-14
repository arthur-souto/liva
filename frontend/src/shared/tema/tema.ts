export type Tema = "claro" | "escuro";

export const CHAVE_DO_TEMA = "liva-tema";

export const SCRIPT_DO_TEMA = `(function(){try{var s=localStorage.getItem("${CHAVE_DO_TEMA}");var t=s==="claro"||s==="escuro"?s:(window.matchMedia("(prefers-color-scheme: dark)").matches?"escuro":"claro");document.documentElement.setAttribute("data-tema",t);}catch(e){}})();`;

export function temaAtual(): Tema {
  return document.documentElement.getAttribute("data-tema") === "escuro" ? "escuro" : "claro";
}

export function temaEscolhido(): Tema | null {
  try {
    const salvo = window.localStorage.getItem(CHAVE_DO_TEMA);
    return salvo === "claro" || salvo === "escuro" ? salvo : null;
  } catch {
    return null;
  }
}

export function aplicarTema(tema: Tema, salvar: boolean): void {
  document.documentElement.setAttribute("data-tema", tema);
  if (!salvar) return;
  try {
    window.localStorage.setItem(CHAVE_DO_TEMA, tema);
  } catch {
    return;
  }
}
