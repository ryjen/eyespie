#!/usr/bin/env bash
set -euo pipefail

bundletool_version="1.18.3"
bundletool_sha256="a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29"
tool_dir="build/tools"
report_dir="build/reports/android-bundle"
model_name="mobilenet_v3_small_100_224_embedder.tflite"
model_entry="base/assets/${model_name}"
staged_model="eyespie/src/androidMain/assets/${model_name}"

python3 scripts/stage_image_embedder_model.py stage --target android
printf 'IMAGE_EMBEDDER_BYTES=%s\n' "$(wc -c < "$staged_model" | tr -d ' ')"

./gradlew \
  :app:bundleDebug \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache

shopt -s nullglob
bundles=(eyespie/build/outputs/bundle/debug/*.aab)
if (( ${#bundles[@]} != 1 )); then
  printf 'expected exactly one debug AAB, found %d\n' "${#bundles[@]}" >&2
  printf 'candidate: %s\n' "${bundles[@]:-none}" >&2
  exit 1
fi

aab_path="${bundles[0]}"
mkdir -p "$tool_dir" "$report_dir"

bundletool_path="$tool_dir/bundletool.jar"
if [[ ! -f "$bundletool_path" ]] || ! printf '%s  %s\n' "$bundletool_sha256" "$bundletool_path" | sha256sum --check --strict --status; then
  curl --fail --location --proto '=https' --tlsv1.2 \
    --output "$bundletool_path" \
    "https://github.com/google/bundletool/releases/download/${bundletool_version}/bundletool-all-${bundletool_version}.jar"
fi
printf '%s  %s\n' "$bundletool_sha256" "$bundletool_path" | sha256sum --check --strict

manifest_path="$report_dir/model-pack-manifest.xml"
report_path="$report_dir/topology.md"
packaged_model_path="$report_dir/$model_name"

java -jar "$bundletool_path" dump manifest \
  --bundle="$aab_path" \
  --module=model_pack \
  > "$manifest_path"

python3 scripts/validate_android_bundle.py \
  --aab "$aab_path" \
  --model-pack-manifest "$manifest_path" \
  --report "$report_path"

python3 - "$aab_path" "$model_entry" "$packaged_model_path" <<'PY'
from pathlib import Path
import sys
import zipfile

aab = Path(sys.argv[1])
entry = sys.argv[2]
destination = Path(sys.argv[3])
with zipfile.ZipFile(aab) as archive:
    matches = [name for name in archive.namelist() if name == entry]
    if len(matches) != 1:
        raise SystemExit(f"expected exactly one packaged image embedder at {entry}, found {len(matches)}")
    destination.write_bytes(archive.read(entry))
PY
python3 scripts/stage_image_embedder_model.py verify-file "$packaged_model_path"

printf 'AAB_PATH=%s\n' "$aab_path"
printf 'TOPOLOGY_REPORT=%s\n' "$report_path"
printf 'IMAGE_EMBEDDER_ENTRY=%s\n' "$model_entry"
cat "$report_path"
