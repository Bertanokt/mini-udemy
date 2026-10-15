"use client";

import { useEffect, useState } from "react";
import { getHealth } from "@/lib/api/health";

type State = { kind: "loading" } | { kind: "up"; service: string } | { kind: "down" };

export function HealthStatus() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    getHealth()
      .then((h) => setState({ kind: "up", service: h.service }))
      .catch(() => setState({ kind: "down" }));
  }, []);

  if (state.kind === "loading") return <p>Backend kontrol ediliyor…</p>;
  if (state.kind === "down") return <p className="status-down">Backend&apos;e ulaşılamadı.</p>;
  return <p className="status-up">Backend çalışıyor ({state.service}).</p>;
}
