"use client";

import { useEffect, useState } from "react";

export function useAgora(intervaloMs = 1000): number {
  const [agora, setAgora] = useState(() => Date.now());

  useEffect(() => {
    const id = window.setInterval(() => setAgora(Date.now()), intervaloMs);
    return () => window.clearInterval(id);
  }, [intervaloMs]);

  return agora;
}
