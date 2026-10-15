#!/usr/bin/env bash
# "Mini Udemy" GitHub Project'ini (board) kurar ve repodaki tüm issue'ları ekler.
#
# - Project yoksa oluşturur ve repoya bağlar.
# - Status alanının seçeneklerini issues.json'daki listeye (Backlog, Bu Hafta, In Progress, Review, Done) eşitler.
#   Seçenekler zaten aynıysa dokunmaz (değiştirmek mevcut kartların durumunu sıfırlar).
# - Board'a eklenmemiş issue'ları ekler. Durumu boş olan kartlara: CURRENT_MILESTONE'dakiler "Bu Hafta", diğerleri "Backlog".
#   Ekibin taşıdığı kartların durumuna dokunmaz.
#
# Kullanım:
#   scripts/setup-board.sh
#   CURRENT_MILESTONE="Hafta 2: Kurs yönetimi + kayıt" scripts/setup-board.sh   # yeni haftada boş kartlar için
#
# Gereken: gh (repo + project scope), jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/issues.json"
REPO="${REPO:-$(jq -r '.repo' "$CONFIG")}"
OWNER="$(jq -r '.project.owner' "$CONFIG")"
TITLE="$(jq -r '.project.title' "$CONFIG")"
CURRENT_MILESTONE="${CURRENT_MILESTONE:-$(jq -r '.milestones[0].title' "$CONFIG")}"

log() { printf '%s\n' "$*" >&2; }

# ---------------------------------------------------------------- project
number=$(gh project list --owner "$OWNER" --format json --limit 100 \
  | jq -r --arg t "$TITLE" '.projects[] | select(.title == $t) | .number' | head -1)
if [[ -z "$number" ]]; then
  number=$(gh project create --owner "$OWNER" --title "$TITLE" --format json --jq '.number')
  log "+ Project oluşturuldu: #$number"
else
  log "= Project var: #$number"
fi
gh project link "$number" --owner "$OWNER" --repo "$REPO" >/dev/null 2>&1 || true

project_id=$(gh project view "$number" --owner "$OWNER" --format json --jq '.id')
project_url=$(gh project view "$number" --owner "$OWNER" --format json --jq '.url')

# ---------------------------------------------------------------- Status alanı
fields=$(gh project field-list "$number" --owner "$OWNER" --format json)
status_field=$(jq -c '.fields[] | select(.name == "Status")' <<<"$fields")
status_field_id=$(jq -r '.id' <<<"$status_field")
want=$(jq -c '.project.statuses' "$CONFIG")
have=$(jq -c '[.options[].name]' <<<"$status_field")

if [[ "$want" != "$have" ]]; then
  colors='{"Backlog":"GRAY","Bu Hafta":"BLUE","In Progress":"YELLOW","Review":"PURPLE","Done":"GREEN"}'
  options=$(jq -c --argjson c "$colors" 'map({name: ., color: ($c[.] // "GRAY"), description: ""})' <<<"$want")
  jq -n --arg field "$status_field_id" --argjson options "$options" '{
    query: "mutation($field: ID!, $options: [ProjectV2SingleSelectFieldOptionInput!]) { updateProjectV2Field(input: {fieldId: $field, singleSelectOptions: $options}) { projectV2Field { ... on ProjectV2SingleSelectField { id } } } }",
    variables: {field: $field, options: $options}
  }' | gh api graphql --input - >/dev/null
  log "+ Status seçenekleri: $(jq -r 'join(" → ")' <<<"$want")"
  fields=$(gh project field-list "$number" --owner "$OWNER" --format json)
  status_field=$(jq -c '.fields[] | select(.name == "Status")' <<<"$fields")
else
  log "= Status seçenekleri zaten doğru"
fi
opt_id() { jq -r --arg n "$1" '.options[] | select(.name == $n) | .id' <<<"$status_field"; }
backlog_id=$(opt_id "Backlog")
thisweek_id=$(opt_id "Bu Hafta")

# ---------------------------------------------------------------- kartlar
items=$(gh project item-list "$number" --owner "$OWNER" --format json --limit 1000)
issues=$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,url,milestone)

added=0; to_week=0; to_backlog=0
while read -r issue; do
  url=$(jq -r '.url' <<<"$issue")
  num=$(jq -r '.number' <<<"$issue")
  ms=$(jq -r '.milestone.title // ""' <<<"$issue")

  item=$(jq -c --argjson n "$num" '.items[] | select(.content.type == "Issue" and .content.number == $n)' <<<"$items")
  if [[ -z "$item" ]]; then
    item_id=$(gh project item-add "$number" --owner "$OWNER" --url "$url" --format json --jq '.id')
    status=""
    added=$((added + 1))
  else
    item_id=$(jq -r '.id' <<<"$item")
    status=$(jq -r '.status // ""' <<<"$item")
  fi

  [[ -n "$status" ]] && continue
  if [[ "$ms" == "$CURRENT_MILESTONE" ]]; then opt="$thisweek_id"; to_week=$((to_week + 1))
  else opt="$backlog_id"; to_backlog=$((to_backlog + 1)); fi
  gh project item-edit --id "$item_id" --project-id "$project_id" \
    --field-id "$status_field_id" --single-select-option-id "$opt" >/dev/null
done < <(jq -c 'sort_by(.number)[]' <<<"$issues")

log "= Kartlar: $added yeni eklendi; durum atanan: $to_week → Bu Hafta, $to_backlog → Backlog"
log "Board: $project_url"
