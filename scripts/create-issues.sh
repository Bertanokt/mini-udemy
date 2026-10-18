#!/usr/bin/env bash
# Etiketleri, milestone'ları ve backlog issue'larını scripts/issues.json'dan oluşturur.
#
# Tekrar çalıştırılabilir:
#   - etiketler --force ile güncellenir,
#   - milestone'lar başlığa göre, issue'lar gövdedeki gizli anahtara (<!-- key: ... -->) ya da başlığa göre aranır;
#     varsa yeniden oluşturulmaz.
#   - --sync verilirse var olan milestone'ların açıklama/bitiş tarihi ve issue'ların başlık, gövde, etiket, milestone'u
#     issues.json'a eşitlenir. Ekibin işaretlediği kabul kriterleri ([x]) korunur.
# "Blocked by #X" numaraları issues.json'daki anahtarlardan çözülür. Bağımlılık listede sonra gelen bir issue'ya da
# işaret edebilir: önce eksik issue'lar açılır, sonra gövdeler gerçek numaralarla yazılır.
#
# Kullanım:
#   scripts/create-issues.sh                  # issues.json'daki repo
#   scripts/create-issues.sh --sync           # var olanları da issues.json'a eşitle
#   scripts/create-issues.sh --prune-labels   # issues.json'da olmayan etiketleri sil
#   REPO=kullanici/repo scripts/create-issues.sh
#
# Gereken: gh (repo scope), jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/issues.json"
REPO="${REPO:-$(jq -r '.repo' "$CONFIG")}"
MAP_FILE="$SCRIPT_DIR/.issue-map.tsv"   # anahtar <TAB> issue no (gitignore'da; ilk eşleşme için yardımcı)
PRUNE_LABELS=false
SYNC=false
for arg in "$@"; do
  case "$arg" in
    --prune-labels) PRUNE_LABELS=true ;;
    --sync) SYNC=true ;;
    *) echo "Bilinmeyen argüman: $arg" >&2; exit 1 ;;
  esac
done

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
log "== Milestone'lar"
existing_ms=$(gh api "repos/$REPO/milestones?state=all&per_page=100")
jq -c '.milestones[] | .due = (.dueDate + "T12:00:00Z")' "$CONFIG" | while read -r ms; do
  title=$(jq -r '.title' <<<"$ms")
  desc=$(jq -r '.description' <<<"$ms")
  due=$(jq -r '.due' <<<"$ms")
  current=$(jq -c --arg t "$title" 'map(select(.title == $t)) | .[0] // empty' <<<"$existing_ms")
  if [[ -z "$current" ]]; then
    gh api "repos/$REPO/milestones" -X POST -f title="$title" -f description="$desc" -f due_on="$due" >/dev/null
    log "  + $title (bitiş ${due:0:10})"
  elif $SYNC && [[ "$(jq -r '.description' <<<"$current")" != "$desc" || "$(jq -r '.due_on[0:10]' <<<"$current")" != "${due:0:10}" ]]; then
    gh api "repos/$REPO/milestones/$(jq -r '.number' <<<"$current")" -X PATCH \
      -f description="$desc" -f due_on="$due" >/dev/null
    log "  ~ $title (bitiş ${due:0:10})"
  else
    log "  = $title"
  fi
done

# ---------------------------------------------------------------- issue'lar
existing=$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,title,body,labels,milestone)
touch "$MAP_FILE"
old_map=$(cat "$MAP_FILE")
: >"$MAP_FILE"

lookup() { awk -F'\t' -v k="$1" '$1==k {print $2}' "$MAP_FILE"; }
ms_title() { jq -r --arg k "$1" '.milestones[] | select(.key == $k) | .title' "$CONFIG"; }

find_existing() { # key title -> number
  local key=$1 title=$2 n
  n=$(jq -r --arg m "<!-- key: $key -->" 'map(select(.body | contains($m))) | .[0].number // empty' <<<"$existing")
  [[ -z "$n" ]] && n=$(awk -F'\t' -v k="$key" '$1==k {print $2}' <<<"$old_map")
  [[ -z "$n" ]] && n=$(jq -r --arg t "$title" 'map(select(.title == $t)) | .[0].number // empty' <<<"$existing")
  printf '%s' "$n"
}

render_body() { # issue-json [mevcut gövde] -> gövde (stdout)
  local issue=$1 current=${2:-} deps="" n why key
  key=$(jq -r '.key' <<<"$issue")
  for dep in $(jq -r '.blockedBy // [] | .[]' <<<"$issue"); do
    n=$(lookup "$dep")
    [[ -z "$n" ]] && { log "HATA: '$key' bilinmeyen '$dep' anahtarına bağlı"; exit 1; }
    deps+="${deps:+, }#$n"
  done
  printf '## Açıklama\n\n%s\n\n' "$(jq -r '.description' <<<"$issue")"
  why=$(jq -r '.why // empty' <<<"$issue")
  [[ -n "$why" ]] && printf '## Neden bu desen?\n\n%s\n\n' "$why"
  printf '## Kabul kriterleri\n\n'
  jq -r '.criteria[]' <<<"$issue" | while IFS= read -r c; do
    if grep -qxF -- "- [x] $c" <<<"$current"; then printf -- '- [x] %s\n' "$c"; else printf -- '- [ ] %s\n' "$c"; fi
  done
  printf '\n## Bağımlılık\n\n'
  if [[ -n "$deps" ]]; then printf 'Blocked by %s\n' "$deps"; else printf 'Yok\n'; fi
  printf '\n<!-- key: %s -->\n' "$key"
}

count=$(jq '.issues | length' "$CONFIG")

# 1. geçiş: eşleştir, eksikleri aç (gövde 2. geçişte yazılır)
log "== Issue'lar: eşleştirme ve oluşturma"
created=0
new_keys=""
for ((i = 0; i < count; i++)); do
  issue=$(jq -c --argjson i "$i" '.issues[$i]' "$CONFIG")
  key=$(jq -r '.key' <<<"$issue")
  title=$(jq -r '.title' <<<"$issue")
  number=$(find_existing "$key" "$title")
  if [[ -z "$number" ]]; then
    args=(--repo "$REPO" --title "$title" --body "Hazırlanıyor… <!-- key: $key -->")
    for label in $(jq -r '.labels[]' <<<"$issue"); do args+=(--label "$label"); done
    ms_key=$(jq -r '.milestone // empty' <<<"$issue")
    [[ -n "$ms_key" ]] && args+=(--milestone "$(ms_title "$ms_key")")
    url=$(gh issue create "${args[@]}")
    number="${url##*/}"
    new_keys+="$key "
    created=$((created + 1))
    log "  + #$number $title"
    sleep 1   # GitHub'ın içerik oluşturma hız sınırı için
  fi
  printf '%s\t%s\n' "$key" "$number" >>"$MAP_FILE"
done

# 2. geçiş: gövdeleri (ve --sync ile başlık/etiket/milestone'u) yaz
log "== Issue'lar: gövde ve alanlar"
updated=0
existing=$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,title,body,labels,milestone)
for ((i = 0; i < count; i++)); do
  issue=$(jq -c --argjson i "$i" '.issues[$i]' "$CONFIG")
  key=$(jq -r '.key' <<<"$issue")
  number=$(lookup "$key")
  is_new=false
  [[ " $new_keys " == *" $key "* ]] && is_new=true
  $SYNC || $is_new || continue

  cur=$(jq -c --argjson n "$number" 'map(select(.number == $n)) | .[0]' <<<"$existing")
  cur_body=$(jq -r '.body' <<<"$cur")
  want_body=$(render_body "$issue" "$cur_body")
  want_title=$(jq -r '.title' <<<"$issue")
  want_labels=$(jq -r '.labels | sort | join(",")' <<<"$issue")
  cur_labels=$(jq -r '[.labels[].name] | sort | join(",")' <<<"$cur")
  ms_key=$(jq -r '.milestone // empty' <<<"$issue")
  want_ms=""; [[ -n "$ms_key" ]] && want_ms=$(ms_title "$ms_key")
  cur_ms=$(jq -r '.milestone.title // ""' <<<"$cur")

  args=()
  changes=()
  if [[ "$cur_body" != "$want_body" ]]; then
    body_file=$(mktemp); printf '%s\n' "$want_body" >"$body_file"
    args+=(--body-file "$body_file"); changes+=(gövde)
  fi
  [[ "$(jq -r '.title' <<<"$cur")" != "$want_title" ]] && { args+=(--title "$want_title"); changes+=(başlık); }
  if [[ "$cur_labels" != "$want_labels" ]]; then
    want_sorted=$(tr ',' '\n' <<<"$want_labels" | LC_ALL=C sort)
    cur_sorted=$(tr ',' '\n' <<<"$cur_labels" | LC_ALL=C sort)
    for l in $(LC_ALL=C comm -13 <(printf '%s\n' "$want_sorted") <(printf '%s\n' "$cur_sorted")); do args+=(--remove-label "$l"); done
    for l in $(LC_ALL=C comm -23 <(printf '%s\n' "$want_sorted") <(printf '%s\n' "$cur_sorted")); do args+=(--add-label "$l"); done
    changes+=(etiket)
  fi
  if [[ "$cur_ms" != "$want_ms" ]]; then
    if [[ -n "$want_ms" ]]; then args+=(--milestone "$want_ms"); else args+=(--remove-milestone); fi
    changes+=(milestone)
  fi
  [[ ${#args[@]} -eq 0 ]] && continue

  gh issue edit "$number" --repo "$REPO" "${args[@]}" >/dev/null
  [[ -n "${body_file:-}" ]] && rm -f "$body_file" && unset body_file
  $is_new || { updated=$((updated + 1)); log "  ~ #$number ${changes[*]}"; }
done

log "== Bitti: $created yeni, $updated güncellendi. Anahtar → numara: $MAP_FILE"
