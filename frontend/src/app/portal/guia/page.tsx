import type { Metadata } from "next";
import { GuiaDoPortal } from "@/features/portal/GuiaDoPortal";

export const metadata: Metadata = { title: "Como usar" };

export default function Page() {
  return <GuiaDoPortal />;
}
