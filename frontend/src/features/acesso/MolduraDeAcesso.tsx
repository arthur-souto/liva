import { CalendarCheck, MessageCircle, Stethoscope } from "lucide-react";
import Image from "next/image";
import type { ReactNode } from "react";
import { Logo } from "@/shared/ui/Logo";
import estilos from "./MolduraDeAcesso.module.css";

const DESTAQUES = [
  { Icone: Stethoscope, texto: "Registro da triagem e dos encaminhamentos" },
  { Icone: CalendarCheck, texto: "Agenda dos especialistas sem conflito de horário" },
  { Icone: MessageCircle, texto: "Confirmação das consultas pelo WhatsApp" },
];

export function MolduraDeAcesso({ children }: { children: ReactNode }) {
  return (
    <div className={estilos.moldura}>
      <section className={estilos.vitrine} aria-hidden="true">
        <Image
          src="/login/triagem-pressao.webp"
          alt=""
          fill
          priority
          sizes="50vw"
          className={estilos.foto}
        />
        <div className={estilos.vitrineConteudo}>
          <Logo tamanho="grande" invertido />
          <div className={estilos.chamada}>
            <p className={estilos.titulo}>Seu cuidado, mais perto.</p>
            <ul className={estilos.destaques}>
              {DESTAQUES.map(({ Icone, texto }) => (
                <li key={texto}>
                  <Icone size={18} />
                  {texto}
                </li>
              ))}
            </ul>
          </div>
          <p className={estilos.credito}>Foto: CDC / Unsplash</p>
        </div>
      </section>

      <section className={estilos.painel}>
        <div className={estilos.logoMovel}>
          <Logo tamanho="grande" />
        </div>
        <div className={estilos.formulario}>{children}</div>
        <p className={estilos.rodape}>Liva · Rede de atenção do SUS</p>
      </section>
    </div>
  );
}
