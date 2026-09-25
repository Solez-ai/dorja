# DORJA × Needle Phase 2 — fine-tuning kit

Everything needed to fine-tune [Needle 3](https://github.com/cactus-compute/needle)
on DORJA's tool surface and swap the tuned model into the app.

```
tools.json          the 4-tool surface (mirrors DorjaAssistant.kt exactly)
generate_data.py    seed-data generator -> data/local/*.jsonl + data/platform/*.jsonl
data/local/         {"query", "tools", "answers"} format  -> needle finetune (local LoRA)
data/platform/      OpenAI chat "messages"+"tools" format -> needle platform finetune
```

The generator's 67 seed examples (5 languages, 17% off-topic refusals,
evidenced-only arguments) prove the pipeline end to end. For a production-grade
tune, scale up with the platform generator (step 2b) before training.

## 1. Install

```sh
pip install "cactus-needle[train]"      # local LoRA path (JAX, CPU)
pip install "cactus-needle[train,gpu]" # CUDA — or [train,metal] on Apple Silicon
```

## 2a. Regenerate the seed data (optional)

```sh
python3 generate_data.py
```

Reproducible (seeded); outputs `train/validation/test` JSONL with a 10% val +
10% test split and ~17% off-topic refusals (the model must learn to say
nothing rather than guess).

## 2b. Scale up with the platform generator (recommended)

The hosted generator creates natural, tool-aware examples from `tools.json` —
100 to 10,000 per run. **This spends platform allowance — ask before running.**

```sh
export NEEDLE_API_KEY=needle_ft_...        # from cactuscompute.com dashboard
needle platform generate --tools tools.json --examples 2000 --suffix dorja --out ./data/gen
```

The generated files are chat-format, so they work for **both** paths. Merge
with the seed data if you like (`cat data/gen/*.jsonl > ...`), then split
train/validation/test roughly 90/5/5.

## 3a. Path A — local LoRA (free, your machine)

```sh
needle finetune data/local/train.jsonl --epochs 10 --out adapter.safetensors
# optionally with synthetic top-up:
needle finetune data/local/train.jsonl --epochs 10 --generate 300 --out adapter.safetensors
# useful flags: --lora-rank 16 --lr 1e-4 --batch-size 16 --max-len 1024 --val-split 0.1
```

Reads both its native `query/answers` format and single-turn chat lines.
Merges at export, confidence head untouched (confidence will read `None`).

```sh
needle build --lora adapter.safetensors --layers 8 --out dorja-8L.cact
```

`--layers 8` (≈8–10 MB) is the sensible device rung to start with; test
smaller rungs if you target low-end devices. To publish:

```sh
export NEEDLE_HF_REPO=<you>/dorja-needle
needle build --lora adapter.safetensors --layers 8 --bits 4 --upload
```

## 3b. Path B — platform fine-tune (recommended: calibrated confidence, 2-bit, every depth scored)

**Spends one platform job at submission — confirm before running.**

```sh
needle platform finetune data/platform/train.jsonl data/platform/validation.jsonl data/platform/test.jsonl --suffix dorja --out ./models
```

Watch it: `needle platform jobs <ftjob-id> --wait --out ./models`. When done,
`./models` holds every depth as `<name>-<depth>L.cact`, and `job["evaluations"]`
gives validation/test accuracy per depth — pick the smallest depth whose test
accuracy plateaus (8 is the usual knee).

Single-file alternative: `needle download model-<id> --depth 8 --out ./models`.

## 4. Evaluate before shipping

Harness to run against the tuned weights (and the base for comparison):

```python
import json, needle
from pathlib import Path

tools = json.loads(Path("tools.json").read_text(encoding="utf-8"))
agent = needle.Needle(tools=tools, weights="models/dorja-8L.cact")

test = [json.loads(l) for l in Path("data/local/test.jsonl").read_text(encoding="utf-8").splitlines()]
correct = 0
for ex in test:
    r = agent.complete(ex["query"])
    expected = ex["answers"]
    got = [{"name": c["name"], "arguments": c["arguments"]} for c in r["function_calls"]]
    ok = (got == expected) or (not expected and not got)
    correct += ok
    if not ok:
        print("MISS:", ex["query"], "->", got, "conf:", r.get("confidence"))
print(f"{correct}/{len(test)} exact-match")
```

Platform jobs already report per-depth test accuracy in `job["evaluations"]`;
use this harness for local LoRA or for a final sanity pass.

## 5. Swap the tuned model into the Android app

DorjaAssistant currently bootstraps the stock Cactus SDK model
(`qwen3-0.6`) via `CactusModelManager`. Once a tuned `.cact` exists:

1. **Ship the archive** in the app (`app/src/main/assets/needle/dorja-8L.cact`,
   ~8–10 MB at depth 8) or download it from your HF repo on first run —
   the same pattern `CactusModelManager` already uses, just pointed at your
   published file.
2. **Bind it** instead of the stock model: construct the Needle engine with
   `weights` pointing at the shipped/downloaded `.cact` (via the C API
   `needle_load(bytes)` bridge or the SDK's equivalent), then re-run the
   confidence-gating pattern from the Needle docs: act at ≥ threshold
   (suggest 0.7), confirm below, refuse on empty calls — which
   `DorjaAssistant.Turn.Unmatched` already handles.
3. **Verify** with the same test split on device before release.

Note: the engine cannot unload weights — construct the tuned agent before any
base-model agent in the same process, or keep them in separate processes.

## Reference

- Needle repo: https://github.com/cactus-compute/needle (`llms.txt` is the canonical API reference)
- Guides: fine-tuning, tool design, confidence — https://cactuscompute.com/blog/finetuning-needle
- Platform API: https://cactuscompute.com/llms/api
