#!/bin/bash
# session-start.sh — Reads state.json + current_scene.md → injects context
# Called automatically at the start of every work session.
# Purpose: Prevents agents from starting cold, losing context between sessions.

STATE_FILE=".agents/state.json"
SCENE_FILE="given_circumstances/current_scene.md"

echo "[HOOK] === SESSION START ==="
echo "[HOOK] Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

if [ -f "$STATE_FILE" ]; then
    echo "[HOOK] State file found:"
    echo "[HOOK]   Last session: $(jq -r '.last_session // "none"' "$STATE_FILE")"
    echo "[HOOK]   Active branch: $(jq -r '.active_branch // "none"' "$STATE_FILE")"
    echo "[HOOK]   Files touched: $(jq -r '.files_touched // [] | join(", ")' "$STATE_FILE")"
    
    PENDING=$(jq -r '.pending_handoff // "none"' "$STATE_FILE")
    if [ "$PENDING" != "none" ] && [ "$PENDING" != "" ]; then
        echo "[HOOK]   ⚠ PENDING HANDOFF: $PENDING"
    fi
    
    LAST_FAIL=$(jq -r '.last_failure // ""' "$STATE_FILE")
    if [ "$LAST_FAIL" != "" ]; then
        echo "[HOOK]   ⚠ LAST FAILURE: $LAST_FAIL"
    fi
else
    echo "[HOOK] No state file — fresh session"
fi

if [ -f "$SCENE_FILE" ]; then
    LINES=$(wc -l < "$SCENE_FILE")
    echo "[HOOK] Scene file: $LINES lines"
    echo "[HOOK] Last 10 lines:"
    tail -10 "$SCENE_FILE" | sed 's/^/   /'
else
    echo "[HOOK] No scene file yet"
fi

echo "[HOOK] === SESSION READY ==="
