#!/usr/bin/env bash
# Ekip kurulumu: collaborator davetleri. Branch protection VARSAYILAN OLARAK UYGULANMAZ.
#
# Kullanım:
#   scripts/setup-team.sh kullanici1 kullanici2 kullanici3 kullanici4   # yalnız davet
#   scripts/setup-team.sh --protection                                   # yalnız branch protection (GitHub Pro açıldıktan sonra)
#
# Davetler idempotent: zaten collaborator olan ya da daveti bekleyen kullanıcıya tekrar davet gitmez.
# Kullanıcı adı GitHub'da yoksa script hiçbir davet göndermeden durur.
#
# --protection kuralları (private repoda repo sahibinde GitHub Pro ya da Team planı gerekir):
#   - main'e değişiklik yalnız PR ile (admin dahil)
#   - en az 1 onay; yeni commit gelince eski onay düşer
#   - CI'daki iki job yeşil olmalı: "backend (build + test)", "frontend (lint + build)"
#   - force push ve branch silme kapalı; yalnız squash merge, merge sonrası branch silinir
#
# Gereken: gh (repo scope), jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/issues.json"
REPO="${REPO:-$(jq -r '.repo' "$CONFIG")}"

log() { printf '%s\n' "$*" >&2; }

protection=false
users=()
for arg in "$@"; do
  case "$arg" in
    --protection) protection=true ;;
    -*) log "Bilinmeyen seçenek: $arg"; exit 1 ;;
    *) users+=("${arg#@}") ;;
  esac
done

if $protection && [[ ${#users[@]} -gt 0 ]]; then
  log "Davet ve --protection ayrı çalıştırılır."; exit 1
fi

if ! $protection; then
  if [[ ${#users[@]} -eq 0 ]]; then
    log "Kullanım: $0 kullanici1 kullanici2 ...  |  $0 --protection"
    exit 1
  fi
  # Önce hepsini doğrula; biri bile yoksa hiç davet gönderme
  for u in "${users[@]}"; do
    gh api "users/$u" --jq .login >/dev/null 2>&1 || { log "HATA: GitHub kullanıcısı bulunamadı: $u (hiç davet gönderilmedi)"; exit 1; }
  done
  owner="${REPO%%/*}"
  collaborators=$(gh api "repos/$REPO/collaborators?per_page=100" --jq '.[].login')
  pending=$(gh api "repos/$REPO/invitations?per_page=100" --jq '.[].invitee.login')
  log "== Collaborator davetleri ($REPO, yetki: push)"
  for u in "${users[@]}"; do
    if [[ "$(tr '[:upper:]' '[:lower:]' <<<"$u")" == "$(tr '[:upper:]' '[:lower:]' <<<"$owner")" ]]; then log "  = $u repo sahibi, atlandı"; continue; fi
    if grep -qixF "$u" <<<"$collaborators"; then log "  = $u zaten collaborator"; continue; fi
    if grep -qixF "$u" <<<"$pending"; then log "  = $u için davet zaten bekliyor"; continue; fi
    gh api "repos/$REPO/collaborators/$u" -X PUT -f permission=push >/dev/null
    log "  + $u davet edildi (daveti GitHub bildiriminden ya da e-postadan kabul etmeli)"
  done
  exit 0
fi

log "== main branch protection"
jq -n '{
  required_status_checks: {
    strict: false,
    contexts: ["backend (build + test)", "frontend (lint + build)"]
  },
  enforce_admins: true,
  required_pull_request_reviews: {
    required_approving_review_count: 1,
    dismiss_stale_reviews: true
  },
  restrictions: null,
  allow_force_pushes: false,
  allow_deletions: false
}' | gh api "repos/$REPO/branches/main/protection" -X PUT --input - >/dev/null
log "  ✓ PR zorunlu, 1 onay, CI (backend + frontend) yeşil olmalı"

# Merge sonrası branch'ler otomatik silinsin; geçmiş temiz kalsın diye yalnız squash merge.
gh api "repos/$REPO" -X PATCH \
  -F delete_branch_on_merge=true \
  -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false >/dev/null
log "  ✓ Merge ayarları: yalnız squash, merge sonrası branch silinir"
