/** FR-29: sertifikada resmî akreditasyon iddiası yoktur. Metin API'den gelmez, ön yüzde sabittir (#130 kararı). */
export const CERTIFICATE_NOTICE = "Bu belge platform içi katılım belgesidir, resmî akreditasyon değildir.";

export function CertificateNotice({ className }: { className?: string }) {
  return (
    <p className={className} role="note">
      {CERTIFICATE_NOTICE}
    </p>
  );
}

/** Sertifika tarihi: "18 Kasım 2026". */
export function formatCertificateDate(iso: string): string {
  return new Date(iso).toLocaleDateString("tr-TR", { day: "numeric", month: "long", year: "numeric" });
}
