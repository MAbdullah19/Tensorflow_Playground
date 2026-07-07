#!/usr/bin/env bash
# End-to-end stress test for the playground.
# Assumes the Java backend is reachable at $BASE.

set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
PASS=0
FAIL=0

ok()   { printf "  \033[32mPASS\033[0m %s\n" "$1"; PASS=$((PASS+1)); }
fail() { printf "  \033[31mFAIL\033[0m %s\n" "$1"; FAIL=$((FAIL+1)); }

assert_lt() {
  local name="$1" actual="$2" expected="$3"
  awk -v a="$actual" -v b="$expected" 'BEGIN{exit !(a < b)}' \
    && ok "$name (loss=$actual < $expected)" \
    || fail "$name (loss=$actual >= $expected)"
}

new_session() {
  curl -fs -X POST "$BASE/api/sessions" -H "Content-Type: application/json" -d "$1" \
    | python -c "import sys,json;print(json.load(sys.stdin)['sessionId'])"
}

train() {
  local sid="$1" body="$2"
  curl -fs -X POST "$BASE/api/sessions/$sid/train" -H "Content-Type: application/json" -d "$body" \
    | python -c "import sys,json;d=json.load(sys.stdin);print(d['lossTrain'],d['lossTest'])"
}

cleanup() {
  curl -fs -X DELETE "$BASE/api/sessions/$1" >/dev/null 2>&1
}

echo "1. Health check"
curl -fs "$BASE/health" >/dev/null && ok "/health" || { fail "/health"; exit 1; }

echo
echo "2. Each dataset converges with a reasonable network"
for cfg in \
  '{"dataset":"circle","networkShape":[4,2]} 200 0.05' \
  '{"dataset":"xor","networkShape":[4,2]} 300 0.05' \
  '{"dataset":"gauss","networkShape":[4,2]} 100 0.01' \
  '{"dataset":"spiral","features":["x","y"],"networkShape":[8,8],"activation":"relu"} 500 0.1' \
  '{"problem":"regression","regDataset":"reg-plane","networkShape":[4,2]} 200 0.05' \
  '{"problem":"regression","regDataset":"reg-gauss","networkShape":[6,4],"activation":"relu"} 300 0.1'
do
  read CFG EPOCHS THRESH <<< "$cfg"
  SID=$(new_session "$CFG")
  read TRAIN_LOSS TEST_LOSS <<< "$(train "$SID" "{\"epochs\":$EPOCHS,\"learningRate\":0.03,\"batchSize\":10}")"
  ds=$(echo "$CFG" | python -c "import sys,json;d=json.loads(sys.stdin.read());print(d.get('dataset') or d.get('regDataset'))")
  assert_lt "$ds converges in $EPOCHS epochs" "$TRAIN_LOSS" "$THRESH"
  cleanup "$SID"
done

echo
echo "3. Each activation can fit XOR (some need extra epochs)"
for AC in tanh relu; do
  SID=$(new_session "{\"dataset\":\"xor\",\"activation\":\"$AC\",\"networkShape\":[4,4]}")
  read TR _ <<< "$(train "$SID" '{"epochs":500,"learningRate":0.03,"batchSize":10}')"
  assert_lt "xor fits with $AC" "$TR" "0.1"
  cleanup "$SID"
done
# Sigmoid with default LR converges slower, give it more epochs.
SID=$(new_session '{"dataset":"xor","activation":"sigmoid","networkShape":[6,4]}')
read TR _ <<< "$(train "$SID" '{"epochs":1500,"learningRate":0.3,"batchSize":10}')"
assert_lt "xor fits with sigmoid" "$TR" "0.5"
cleanup "$SID"

echo
echo "4. L1 regularisation kills SOME weights in dense net"
SID=$(new_session '{"dataset":"circle","regularization":"L1","networkShape":[8,8,8]}')
RESP=$(curl -fs -X POST "$BASE/api/sessions/$SID/train" -H "Content-Type: application/json" -d '{"epochs":300,"learningRate":0.03,"regularizationRate":0.005,"batchSize":10}')
DEAD=$(echo "$RESP" | python -c "import sys,json;d=json.load(sys.stdin);print(sum(1 for l in d['links'] if l['dead']))")
TOTAL=$(echo "$RESP" | python -c "import sys,json;d=json.load(sys.stdin);print(len(d['links']))")
ALIVE=$((TOTAL - DEAD))
([ "$DEAD" -gt 0 ] && [ "$ALIVE" -gt 0 ]) && ok "L1 killed $DEAD/$TOTAL weights, kept $ALIVE alive" \
  || fail "L1 dead/total = $DEAD/$TOTAL (expected partial)"
cleanup "$SID"

echo
echo "5. Reset / regenerate / configure flow"
SID=$(new_session '{}')
ITER1=$(curl -fs -X POST "$BASE/api/sessions/$SID/train" -H "Content-Type: application/json" -d '{"epochs":50}' | python -c "import sys,json;print(json.load(sys.stdin)['iter'])")
ITER2=$(curl -fs -X POST "$BASE/api/sessions/$SID/build-network" -H "Content-Type: application/json" -d '{}' | python -c "import sys,json;print(json.load(sys.stdin)['iter'])")
[ "$ITER1" = "50" ] && [ "$ITER2" = "0" ] && ok "build-network resets iter" || fail "build-network doesn't reset (iter1=$ITER1, iter2=$ITER2)"
cleanup "$SID"

echo
echo "6. Boundary endpoint at varying densities"
SID=$(new_session '{"networkShape":[8,8]}')
for D in 10 30 60 100; do
  T0=$(date +%s%N)
  BYTES=$(curl -fs -X POST "$BASE/api/sessions/$SID/boundary" -H "Content-Type: application/json" -d "{\"density\":$D}" | wc -c)
  T1=$(date +%s%N)
  printf "    density=%-3d  %8d bytes  %5d ms\n" "$D" "$BYTES" "$(((T1-T0)/1000000))"
done
ok "boundary endpoint scales without errors"
cleanup "$SID"

echo
echo "7. Concurrent sessions don't interfere"
PIDS=()
for i in $(seq 1 8); do
  ( SID=$(new_session "{\"dataset\":\"circle\"}")
    read TR _ <<< "$(train "$SID" '{"epochs":300}')"
    awk -v a="$TR" 'BEGIN{exit !(a < 0.1)}' && echo "  worker $i loss $TR" || echo "  WORKER $i FAIL loss $TR"
    cleanup "$SID"
  ) &
  PIDS+=($!)
done
T0=$(date +%s%N)
for p in "${PIDS[@]}"; do wait "$p"; done
T1=$(date +%s%N)
ELAPSED=$(((T1-T0)/1000000))
ok "8 concurrent training sessions completed in $ELAPSED ms"

echo
echo "8. Weight / bias overrides"
SID=$(new_session '{}')
STATE=$(curl -fs "$BASE/api/sessions/$SID/state")
SOURCE=$(echo "$STATE" | python -c "import sys,json;print(json.load(sys.stdin)['links'][0]['source'])")
DEST=$(echo   "$STATE" | python -c "import sys,json;print(json.load(sys.stdin)['links'][0]['dest'])")
NEW=$(curl -fs -X POST "$BASE/api/sessions/$SID/weight" -H "Content-Type: application/json" -d "{\"source\":\"$SOURCE\",\"dest\":\"$DEST\",\"weight\":0.42}")
W=$(SOURCE="$SOURCE" DEST="$DEST" python -c "import sys,json,os;d=json.load(sys.stdin);src=os.environ['SOURCE'];dst=os.environ['DEST'];print([l['weight'] for l in d['links'] if l['source']==src and l['dest']==dst][0])" <<< "$NEW")
awk -v a="$W" -v b="0.42" 'BEGIN{exit !(a == b)}' \
  && ok "manual weight override applied (w=$W)" \
  || fail "weight override returned w=$W (expected 0.42)"
NEW2=$(curl -fs -X POST "$BASE/api/sessions/$SID/bias" -H "Content-Type: application/json" -d "{\"id\":\"1\",\"bias\":-0.5}")
B=$(echo "$NEW2" | python -c "import sys,json;d=json.load(sys.stdin);print([n for layer in d['layers'] for n in layer if n['id']=='1'][0]['bias'])")
awk -v a="$B" -v b="-0.5" 'BEGIN{exit !(a == b)}' \
  && ok "manual bias override applied (b=$B)" \
  || fail "bias override returned b=$B (expected -0.5)"
cleanup "$SID"

echo
echo "Summary: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
