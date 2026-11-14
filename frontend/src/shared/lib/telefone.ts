export function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, "");
}

function semCodigoDoPais(digitos: string): string {
  return digitos.length >= 12 && digitos.startsWith("55") ? digitos.slice(2) : digitos;
}

export function formatarTelefone(valor: string | null | undefined): string {
  if (!valor) return "—";
  const d = semCodigoDoPais(apenasDigitos(valor));
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return valor;
}

export function mascararTelefone(valor: string): string {
  const d = apenasDigitos(valor).slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : "";
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

export function telefoneValido(valor: string): boolean {
  const total = apenasDigitos(valor).length;
  return total === 10 || total === 11;
}

export function linkWhatsApp(valor: string): string {
  return `https://wa.me/55${semCodigoDoPais(apenasDigitos(valor))}`;
}
