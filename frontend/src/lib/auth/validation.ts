import type { RegisterRequest } from "@/lib/api/auth";

/**
 * Kayıt formunun istemci tarafı doğrulaması (#17). Kurallar ve mesajlar backend'deki `RegisterRequest` ve
 * `AuthService.register` ile aynı (docs/api/openapi.yaml › RegisterRequest); sunucu yine de her şeyi denetler.
 */
export type RegisterField = keyof RegisterRequest;
export type RegisterErrors = Partial<Record<RegisterField, string>>;

// Sunucudaki @Email'den biraz daha sıkı: "@" ve noktalı bir alan adı ister; boşluk kabul etmez.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** BCrypt 72 bayttan uzun parolayı işlemez; Türkçe harfler UTF-8'de 2 bayt. */
const PASSWORD_MAX_BYTES = 72;

export function validateRegister(form: RegisterRequest): RegisterErrors {
  const errors: RegisterErrors = {};

  const fullName = form.fullName.trim();
  if (!fullName) errors.fullName = "Ad soyad zorunludur.";
  else if (form.fullName.length < 2 || form.fullName.length > 100) errors.fullName = "Ad soyad 2–100 karakter olmalıdır.";

  if (!form.email.trim()) errors.email = "E-posta zorunludur.";
  else if (form.email.length > 254) errors.email = "E-posta en fazla 254 karakter olabilir.";
  else if (!EMAIL_PATTERN.test(form.email)) errors.email = "Geçerli bir e-posta adresi girin.";

  if (!form.password) errors.password = "Parola zorunludur.";
  else if (form.password.length < 8) errors.password = "Parola en az 8 karakter olmalıdır.";
  else if (form.password.length > 72) errors.password = "Parola en fazla 72 karakter olabilir.";
  else if (new TextEncoder().encode(form.password).length > PASSWORD_MAX_BYTES)
    errors.password = "Parola çok uzun: en fazla 72 bayt olabilir (Türkçe harfler 2 bayt sayılır).";

  if (form.role !== "STUDENT" && form.role !== "INSTRUCTOR") errors.role = "Rol zorunludur.";

  return errors;
}
