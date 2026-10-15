#!/usr/bin/env bash
# Etiketleri, milestone'ları ve backlog issue'larını scripts/issues.json'dan oluşturur.
#
# Tekrar çalıştırılabilir:
#   - etiketler --force ile güncellenir,
#   - milestone'lar ve issue'lar başlığa göre aranır; varsa yeniden oluşturulmaz, değiştirilmez.
# "Blocked by #X" numaraları issues.json'daki anahtarlardan çözülür; bağımlılık her zaman listede daha önce gelir.
#
# Kullanım:
#   scripts/create-issues.sh                  # issues.json'daki repo
#   REPO=kullanici/repo scripts/create-issues.sh
#   scripts/create-issues.sh --prune-labels   # GitHub'ın varsayılan, issues.json'da olmayan etiketlerini siler
#
# Gereken: gh (repo scope), jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/issues.json"
REPO="${REPO:-$(jq -r '.repo' "$CONFIG")}"
START_DATE="$(jq -r '.startDate' "$CONFIG")"
MAP_FILE="$SCRIPT_DIR/.issue-map.tsv"   # anahtar <TAB> issue no (gitignore'da)
PRUNE_LABELS=false
[[ "${1:-}" == "--prune-labels" ]] && PRUNE_LABELS=true

log() { printf '%s\n' "$*" >&2; }

# ---------------------------------------------------------------- etiketler
log "== Etiketler ($REPO)"
jq -c '.labels[]' "$CONFIG" | while read -r label; do
  name=$(jq -r '.name' <<<"$label")
  gh label create "$name" --repo "$REPO" \
    --color "$(jq -r '.color' <<<"$label")" \
    --description "$(jq -r '.description' <<<"$label")" \
    --force >/dev/null
  log "  ✓ $name"
done

if $PRUNE_LABELS; then
  wanted=$(jq -r '.labels[].name' "$CONFIG")
  gh label list --repo "$REPO" --limit 200 --json name -q '.[].name' | while read -r name; do
    if ! grep -qxF "$name" <<<"$wanted"; then
      gh label delete "$name" --repo "$REPO" --yes >/dev/null
      log "  - silindi: $name"
    fi
  done
fi

# ---------------------------------------------------------------- milestone'lar
log "== Milestone'lar (başlangıç $START_DATE)"
existing_ms=$(gh api "repos/$REPO/milestones?state=all&per_page=100" --jq '.[].title')
jq -c --arg start "$START_DATE" '
  .milestones[]
  | .due = (($start + "T12:00:00Z" | fromdateiso8601) + .dueOffsetDays * 86400 | todateiso8601)
' "$CONFIG" | while read -r ms; do
  title=$(jq -r '.title' <<<"$ms")
  if grep -qxF "$title" <<<"$existing_ms"; then
    log "  = var: $title"
  else
    gh api "repos/$REPO/milestones" -X POST \
      -f title="$title" \
      -f description="$(jq -r '.description' <<<"$ms")" \
      -f due_on="$(jq -r '.due' <<<"$ms")" >/dev/null
    log "  + $title (bitiş $(jq -r '.due[0:10]' <<<"$ms"))"
  fi
done

# ---------------------------------------------------------------- issue'lar
log "== Issue'lar"
: >"$MAP_FILE"
existing_issues=$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,title)

lookup() { awk -F'\t' -v k="$1" '$1==k {print $2}' "$MAP_FILE"; }

created=0
skipped=0
count=$(jq '.issues | length' "$CONFIG")
for ((i = 0; i < count; i++)); do
  issue=$(jq -c --argjson i "$i" '.issues[$i]' "$CONFIG")
  key=$(jq -r '.key' <<<"$issue")
  title=$(jq -r '.title' <<<"$issue")

  number=$(jq -r --arg t "$title" 'map(select(.title == $t)) | .[0].number // empty' <<<"$existing_issues")
  if [[ -n "$number" ]]; then
    printf '%s\t%s\n' "$key" "$number" >>"$MAP_FILE"
    log "  = #$number $title"
    skipped=$((skipped + 1))
    continue
  fi

  # Bağımlılıkları gerçek issue numarasına çevir
  deps=""
  for dep in $(jq -r '.blockedBy // [] | .[]' <<<"$issue"); do
    n=$(lookup "$dep")
    if [[ -z "$n" ]]; then
      log "HATA: '$key' issue'su '$dep' anahtarına bağlı ama o issue henüz yok (issues.json sırasını kontrol et)"
      exit 1
    fi
    deps+="${deps:+, }#$n"
  done

  body_file=$(mktemp)
  {
    printf '## Açıklama\n\n%s\n\n' "$(jq -r '.description' <<<"$issue")"
    why=$(jq -r '.why // empty' <<<"$issue")
    [[ -n "$why" ]] && printf '## Neden bu desen?\n\n%s\n\n' "$why"
    printf '## Kabul kriterleri\n\n'
    jq -r '.criteria[] | "- [ ] " + .' <<<"$issue"
    printf '\n## Bağımlılık\n\n'
    if [[ -n "$deps" ]]; then printf 'Blocked by %s\n' "$deps"; else printf 'Yok\n'; fi
  } >"$body_file"

  args=(--repo "$REPO" --title "$title" --body-file "$body_file")
  for label in $(jq -r '.labels[]' <<<"$issue"); do args+=(--label "$label"); done
  ms_key=$(jq -r '.milestone // empty' <<<"$issue")
  if [[ -n "$ms_key" ]]; then
    args+=(--milestone "$(jq -r --arg k "$ms_key" '.milestones[] | select(.key == $k) | .title' "$CONFIG")")
  fi

  url=$(gh issue create "${args[@]}")
  rm -f "$body_file"
  number="${url##*/}"
  printf '%s\t%s\n' "$key" "$number" >>"$MAP_FILE"
  log "  + #$number $title${deps:+  (Blocked by $deps)}"
  created=$((created + 1))
  sleep 1   # GitHub'ın içerik oluşturma hız sınırına takılmamak için
done

log "== Bitti: $created yeni, $skipped zaten vardı. Anahtar → numara eşlemesi: $MAP_FILE"
