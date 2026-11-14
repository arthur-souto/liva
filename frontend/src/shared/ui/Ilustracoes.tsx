import estilos from "./Ilustracoes.module.css";

export function PessoasNoPortal() {
  return (
    <svg viewBox="0 0 360 240" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="205" cy="128" r="102" className={estilos.fundo} />
      <circle cx="56" cy="58" r="20" className={estilos.fundoQuente} />
      <circle cx="330" cy="40" r="8" className={estilos.fundoQuente} />
      <ellipse cx="200" cy="216" rx="160" ry="7" className={estilos.chao} />

      <rect x="168" y="48" width="132" height="122" rx="6" className={estilos.papel} />
      <rect x="168" y="48" width="132" height="24" rx="6" className={estilos.primaria} />
      <rect x="168" y="62" width="132" height="10" className={estilos.primaria} />
      <circle cx="190" cy="44" r="4" className={estilos.traco} />
      <circle cx="278" cy="44" r="4" className={estilos.traco} />
      {[0, 1, 2, 3].map((linha) =>
        [0, 1, 2, 3].map((coluna) => (
          <rect
            key={`${linha}-${coluna}`}
            x={180 + coluna * 29}
            y={84 + linha * 21}
            width="20"
            height="14"
            rx="2"
            className={linha === 2 && coluna === 1 ? estilos.destaque : estilos.grade}
          />
        )),
      )}
      <path d="M213 133 l4 4 l7 -8" className={estilos.check} />

      <rect x="56" y="34" width="50" height="28" rx="6" className={estilos.mensagem} />
      <path d="M92 62 l6 8 l2 -8 z" className={estilos.mensagem} />
      <circle cx="70" cy="48" r="3" className={estilos.ponto} />
      <circle cx="81" cy="48" r="3" className={estilos.ponto} />
      <circle cx="92" cy="48" r="3" className={estilos.ponto} />

      <line x1="104" y1="152" x2="100" y2="210" className={estilos.perna} />
      <line x1="120" y1="152" x2="126" y2="210" className={estilos.perna} />
      <path d="M86 156 L90 104 Q112 92 134 104 L138 156 Z" className={estilos.roupaRoxa} />
      <line x1="92" y1="112" x2="86" y2="150" className={estilos.bracoRoxo} />
      <line x1="130" y1="112" x2="142" y2="92" className={estilos.bracoRoxo} />
      <rect x="136" y="70" width="15" height="25" rx="3" className={estilos.primaria} />
      <rect x="138.5" y="73" width="10" height="17" rx="1" className={estilos.tela} />
      <circle cx="112" cy="78" r="17" className={estilos.peleMedia} />
      <path d="M95 76 Q96 58 112 58 Q129 58 130 76 Q122 66 112 67 Q102 66 95 76 Z" className={estilos.cabelo} />

      <line x1="318" y1="156" x2="314" y2="210" className={estilos.perna} />
      <line x1="332" y1="156" x2="338" y2="210" className={estilos.perna} />
      <path d="M300 160 L304 108 Q325 96 346 108 L350 160 Z" className={estilos.roupaLaranja} />
      <line x1="306" y1="116" x2="282" y2="108" className={estilos.bracoLaranja} />
      <line x1="344" y1="116" x2="350" y2="152" className={estilos.bracoLaranja} />
      <circle cx="325" cy="82" r="17" className={estilos.peleEscura} />
      <circle cx="325" cy="62" r="9" className={estilos.cabelo} />
      <path d="M308 82 Q308 64 325 64 Q342 64 342 82 Q336 72 325 72 Q314 72 308 82 Z" className={estilos.cabelo} />
    </svg>
  );
}

export function PessoaComCelular() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="104" r="74" className={estilos.fundo} />
      <circle cx="210" cy="150" r="10" className={estilos.fundoQuente} />

      <rect x="18" y="52" width="62" height="26" rx="6" className={estilos.papel} />
      <rect x="28" y="61" width="34" height="4" rx="2" className={estilos.linhaTexto} />
      <rect x="28" y="68" width="22" height="4" rx="2" className={estilos.linhaTexto} />
      <rect x="160" y="34" width="64" height="26" rx="6" className={estilos.mensagem} />
      <rect x="170" y="43" width="40" height="4" rx="2" className={estilos.linhaTexto} />
      <rect x="170" y="50" width="26" height="4" rx="2" className={estilos.linhaTexto} />
      <rect x="168" y="70" width="40" height="22" rx="6" className={estilos.papel} />
      <path d="M180 81 l4 4 l8 -9" className={estilos.checkVerde} />

      <path d="M74 180 L82 130 Q120 112 158 130 L166 180 Z" className={estilos.roupaRoxa} />
      <line x1="96" y1="160" x2="134" y2="140" className={estilos.bracoPele} />
      <rect x="130" y="112" width="22" height="36" rx="4" className={estilos.primaria} />
      <rect x="133" y="116" width="16" height="25" rx="1" className={estilos.tela} />
      <circle cx="120" cy="94" r="21" className={estilos.peleClara} />
      <path d="M99 96 Q96 70 120 70 Q144 70 141 96 Q138 82 120 80 Q104 82 99 96 Z" className={estilos.cabelo} />
      <path d="M99 96 Q96 116 104 122 L104 96 Z" className={estilos.cabelo} />
      <path d="M141 96 Q144 116 136 122 L136 96 Z" className={estilos.cabelo} />
    </svg>
  );
}

export function PessoasDeMaosDadas() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="104" r="74" className={estilos.fundo} />
      <ellipse cx="120" cy="174" rx="96" ry="5" className={estilos.chao} />
      <path d="M120 48 C112 36 96 42 104 56 L120 70 L136 56 C144 42 128 36 120 48 Z" className={estilos.destaque} />

      <line x1="84" y1="132" x2="82" y2="172" className={estilos.perna} />
      <line x1="96" y1="132" x2="100" y2="172" className={estilos.perna} />
      <path d="M70 136 L74 98 Q90 88 106 98 L110 136 Z" className={estilos.roupaLaranja} />
      <line x1="74" y1="104" x2="62" y2="132" className={estilos.bracoLaranja} />
      <line x1="60" y1="130" x2="56" y2="174" className={estilos.bengala} />
      <circle cx="90" cy="76" r="15" className={estilos.peleClara} />
      <circle cx="90" cy="58" r="7" className={estilos.cabeloGrisalho} />
      <path d="M75 76 Q75 61 90 61 Q105 61 105 76 Q99 68 90 68 Q81 68 75 76 Z" className={estilos.cabeloGrisalho} />

      <line x1="146" y1="128" x2="142" y2="172" className={estilos.perna} />
      <line x1="160" y1="128" x2="166" y2="172" className={estilos.perna} />
      <path d="M130 132 L134 88 Q153 76 172 88 L176 132 Z" className={estilos.roupaRoxa} />
      <line x1="136" y1="96" x2="104" y2="100" className={estilos.bracoRoxo} />
      <line x1="170" y1="96" x2="176" y2="128" className={estilos.bracoRoxo} />
      <circle cx="153" cy="64" r="16" className={estilos.peleEscura} />
      <path d="M137 66 Q134 46 153 46 Q172 46 169 66 Q164 54 153 54 Q142 54 137 66 Z" className={estilos.cabelo} />
    </svg>
  );
}

export function PessoaAcenando() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="100" r="74" className={estilos.fundo} />
      <circle cx="200" cy="40" r="10" className={estilos.fundoQuente} />
      <ellipse cx="120" cy="174" rx="80" ry="5" className={estilos.chao} />
      <rect x="28" y="50" width="56" height="28" rx="6" className={estilos.mensagem} />
      <path d="M70 78 l6 8 l2 -8 z" className={estilos.mensagem} />
      <circle cx="44" cy="64" r="3" className={estilos.ponto} />
      <circle cx="56" cy="64" r="3" className={estilos.ponto} />
      <circle cx="68" cy="64" r="3" className={estilos.ponto} />

      <line x1="114" y1="140" x2="110" y2="172" className={estilos.perna} />
      <line x1="128" y1="140" x2="134" y2="172" className={estilos.perna} />
      <path d="M98 144 L102 98 Q120 86 138 98 L142 146 Z" className={estilos.roupaRoxa} />
      <line x1="104" y1="106" x2="96" y2="140" className={estilos.bracoRoxo} />
      <line x1="136" y1="104" x2="158" y2="72" className={estilos.bracoRoxo} />
      <circle cx="160" cy="66" r="7" className={estilos.peleMedia} />
      <path d="M172 54 q6 8 0 18" className={estilos.aceno} />
      <path d="M180 48 q9 14 0 30" className={estilos.aceno} />
      <circle cx="120" cy="72" r="17" className={estilos.peleMedia} />
      <circle cx="108" cy="58" r="7" className={estilos.cabelo} />
      <circle cx="120" cy="54" r="8" className={estilos.cabelo} />
      <circle cx="132" cy="58" r="7" className={estilos.cabelo} />
    </svg>
  );
}

export function PessoaComCaixaVazia() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="100" r="74" className={estilos.fundo} />
      <ellipse cx="120" cy="174" rx="90" ry="5" className={estilos.chao} />
      <path d="M124 116 L196 116 L196 170 L124 170 Z" className={estilos.caixa} />
      <path d="M124 116 L112 98 L180 98 L196 116 Z" className={estilos.tampa} />
      <path d="M196 116 L210 100 L190 92 Z" className={estilos.tampa} />
      <text x="152" y="88" className={estilos.interrogacao}>
        ?
      </text>

      <line x1="80" y1="140" x2="76" y2="172" className={estilos.perna} />
      <line x1="94" y1="140" x2="100" y2="172" className={estilos.perna} />
      <path d="M64 144 L68 98 Q86 86 104 98 L108 146 Z" className={estilos.roupaLaranja} />
      <line x1="70" y1="106" x2="62" y2="140" className={estilos.bracoLaranja} />
      <line x1="102" y1="106" x2="126" y2="112" className={estilos.bracoLaranja} />
      <circle cx="88" cy="72" r="17" className={estilos.peleClara} />
      <path d="M71 74 Q70 54 88 54 Q106 54 105 74 Q98 62 88 62 Q78 62 71 74 Z" className={estilos.cabelo} />
    </svg>
  );
}

export function PessoaComLupa() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="100" r="74" className={estilos.fundo} />
      <circle cx="36" cy="60" r="10" className={estilos.fundoQuente} />
      <ellipse cx="120" cy="174" rx="80" ry="5" className={estilos.chao} />
      <rect x="40" y="120" width="40" height="6" rx="3" className={estilos.linhaTexto} />
      <rect x="40" y="132" width="28" height="6" rx="3" className={estilos.linhaTexto} />

      <line x1="104" y1="140" x2="100" y2="172" className={estilos.perna} />
      <line x1="118" y1="140" x2="124" y2="172" className={estilos.perna} />
      <path d="M88 144 L92 98 Q110 86 128 98 L132 146 Z" className={estilos.roupaRoxa} />
      <line x1="94" y1="106" x2="86" y2="140" className={estilos.bracoRoxo} />
      <line x1="126" y1="106" x2="146" y2="96" className={estilos.bracoRoxo} />
      <line x1="146" y1="94" x2="156" y2="84" className={estilos.cabo} />
      <circle cx="170" cy="70" r="20" className={estilos.lente} />
      <circle cx="110" cy="72" r="17" className={estilos.peleEscura} />
      <path d="M93 74 Q92 54 110 54 Q128 54 127 74 Q120 62 110 62 Q100 62 93 74 Z" className={estilos.cabelo} />
    </svg>
  );
}

export function ProfissionalComPrancheta() {
  return (
    <svg viewBox="0 0 240 180" className={estilos.ilustracao} aria-hidden="true" focusable="false">
      <circle cx="120" cy="100" r="74" className={estilos.fundo} />
      <circle cx="204" cy="56" r="10" className={estilos.fundoQuente} />
      <ellipse cx="120" cy="174" rx="80" ry="5" className={estilos.chao} />
      <path d="M44 70 h12 v-12 h10 v12 h12 v10 h-12 v12 h-10 v-12 h-12 z" className={estilos.destaque} />

      <line x1="112" y1="140" x2="108" y2="172" className={estilos.perna} />
      <line x1="126" y1="140" x2="132" y2="172" className={estilos.perna} />
      <path d="M94 146 L98 98 Q118 86 138 98 L142 146 Z" className={estilos.jaleco} />
      <path d="M110 92 L118 112 L126 92 Z" className={estilos.roupaRoxa} />
      <path d="M106 96 Q104 118 112 122" className={estilos.estetoscopio} />
      <circle cx="113" cy="124" r="3" className={estilos.primaria} />
      <line x1="100" y1="106" x2="94" y2="140" className={estilos.bracoJaleco} />
      <line x1="136" y1="106" x2="148" y2="122" className={estilos.bracoJaleco} />
      <rect x="146" y="100" width="30" height="40" rx="3" className={estilos.papel} />
      <rect x="154" y="96" width="14" height="7" rx="2" className={estilos.primaria} />
      <rect x="152" y="112" width="18" height="3" rx="1.5" className={estilos.linhaTexto} />
      <rect x="152" y="120" width="14" height="3" rx="1.5" className={estilos.linhaTexto} />
      <path d="M152 130 l3 3 l6 -6" className={estilos.checkVerde} />
      <circle cx="118" cy="70" r="17" className={estilos.peleEscura} />
      <circle cx="118" cy="50" r="8" className={estilos.cabelo} />
      <path d="M101 72 Q100 54 118 54 Q136 54 135 72 Q128 62 118 62 Q108 62 101 72 Z" className={estilos.cabelo} />
    </svg>
  );
}
