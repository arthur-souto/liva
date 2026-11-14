"use client";

import Script from "next/script";

type JanelaComVLibras = Window & { VLibras?: { Widget: new (endereco: string) => unknown } };

function iniciar() {
  const janela = window as JanelaComVLibras;
  if (janela.VLibras) new janela.VLibras.Widget("https://vlibras.gov.br/app");
}

export function Libras() {
  return <Script src="https://vlibras.gov.br/app/vlibras-plugin.js" strategy="afterInteractive" onLoad={iniciar} />;
}
