import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki CertificateResponse ve CertificateVerificationResponse ile aynıdır.

export interface CertificateResponse {
  id: number;
  courseId: number;
  courseTitle: string;
  studentName: string;
  instructorName?: string | null;
  verificationCode: string;
  issuedAt: string;
}

export interface CertificateVerificationResponse {
  valid: boolean;
  courseTitle: string;
  studentName: string;
  issuedAt: string;
}

/** Kursiyerin sertifikaları, en yeni önce (STUDENT). */
export function listMyCertificates(): Promise<CertificateResponse[]> {
  return apiFetch<CertificateResponse[]>("/api/me/certificates");
}

/** Kursiyerin bir sertifikası; başkasınınsa ya da yoksa 404 (ApiError). */
export function getMyCertificate(id: number): Promise<CertificateResponse> {
  return apiFetch<CertificateResponse>(`/api/me/certificates/${id}`);
}

/** Herkese açık doğrulama (girişsiz); geçersiz kod 404 (ApiError). */
export function verifyCertificate(code: string): Promise<CertificateVerificationResponse> {
  return apiFetch<CertificateVerificationResponse>(`/api/certificates/verify/${encodeURIComponent(code)}`);
}
