#!/usr/bin/env bash
# Ekip kurulumu: collaborator davetleri + main branch protection.
# Repo iskeleti main'e girdikten SONRA çalıştırılır; bundan sonra main'e doğrudan push kapanır.
#
# Kullanım:
#   scripts/setup-team.sh kullanici1 kullanici2 kullanici3 kullanici4
#   scripts/setup-team.sh --protection-only      # yalnız branch protection
#
# Branch protection kuralları:
#   - main'e değişiklik yalnız PR ile (admin dahil)
#   - en az 1 onay; yeni commit gelince eski onay düşer
#   - CI'daki iki job yeşil olmalı: "backend (build + test)", "frontend (lint + build)"
#   - force push ve branch silme kapalı
#
# Not: private repoda branch protection için repo sahibinde GitHub Pro (ör. Student Pack) ya da Team planı gerekir.
# Gereken: gh (repo scope), jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/issues.json"
REPO="${REPO:-$(jq -r '.repo' "$CONFIG")}"

log() { printf '%s\n' "$*" >&2; }

protection_only=false
users=()
for arg in "$@"; do
  case "$arg" in
    --protection-only) protection_only=true ;;
    *) users+=("${arg#@}") ;;
  esac
done

if ! $protection_only; then
  if [[ ${#users[@]} -eq 0 ]]; then
    log "Kullanım: $0 kullanici1 kullanici2 ... | --protection-only"
    exit 1
  fi
  log "== Collaborator davetleri ($REPO, yetki: push)"
  for u in "${users[@]}"; do
    gh api "repos/$REPO/collaborators/$u" -X PUT -f permission=push >/dev/null
    log "  ✓ $u davet edildi (daveti GitHub bildiriminden kabul etmeli)"
  done
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
