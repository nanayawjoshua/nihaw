#!/bin/bash
# session-stop.sh — Captures session output, updates state, appends scene
# Called at end of every work session.
# Purpose: Knowledge continuity across sessions and between agents.

STATE_FILE=".agents/state.json"
SCENE_FILE="given_circumstances/current_scene.md"
LESSONS_FILE="given_circumstances/lessons-learned.md"

echo "[HOOK] === SESSION STOP ==="

BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

FILES_CHANGED=$(git diff --name-only HEAD 2>/dev/null || echo "")
FILES_UNSTAGED=$(git diff --name-only 2>/dev/null || echo "")
FILES_STAGED=$(git diff --cached --name-only 2>/dev/null || echo "")
ALL_FILES=$(echo -e "$FILES_CHANGED\n$FILES_UNSTAGED\n$FILES_STAGED" | sort -u | grep -v '^$' | tr '\n' ',' | sed 's/,$//')

# Build JSON-safe files array
FILES_JSON="[]"
if [ "$ALL_FILES" != "" ]; then
    FILES_JSON="["
    FIRST=true
    IFS=','
    for f in $ALL_FILES; do
        if [ "$FIRST" = true ]; then
            FILES_JSON="$FILES_JSON\"$f\""
            FIRST=false
        else
            FILES_JSON="$FILES_JSON,\"$f\""
        fi
    done
    unset IFS
    FILES_JSON="$FILES_JSON]"
fi

# Build state update
cat > "$STATE_FILE.tmp" << EOF
{
  "last_session": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "active_branch": "$BRANCH",
  "files_touched": $FILES_JSON,
  "pending_handoff": "",
  "last_decision": "",
  "last_failure": ""
}
EOF

# Preserve handoff/failure if updating existing state
if [ -f "$STATE_FILE" ]; then
    OLD_HANDOFF=$(jq -r '.pending_handoff // ""' "$STATE_FILE")
    OLD_FAILURE=$(jq -r '.last_failure // ""' "$STATE_FILE")
    OLD_DECISION=$(jq -r '.last_decision // ""' "$STATE_FILE")
    
    cat > "$STATE_FILE.tmp" << EOF
{
  "last_session": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "active_branch": "$BRANCH",
  "files_touched": $FILES_JSON,
  "pending_handoff": "$OLD_HANDOFF",
  "last_decision": "$OLD_DECISION",
  "last_failure": "$OLD_FAILURE"
}
EOF
fi

mv "$STATE_FILE.tmp" "$STATE_FILE"
echo "[HOOK] State saved to $STATE_FILE"
echo "[HOOK] === SESSION END ==="
