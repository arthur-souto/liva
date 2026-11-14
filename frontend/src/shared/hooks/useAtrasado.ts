"use client";

import { useEffect, useState } from "react";

export function useAtrasado<T>(valor: T, atrasoMs = 350): T {
  const [atrasado, setAtrasado] = useState(valor);

  useEffect(() => {
    const id = window.setTimeout(() => setAtrasado(valor), atrasoMs);
    return () => window.clearTimeout(id);
  }, [valor, atrasoMs]);

  return atrasado;
}
