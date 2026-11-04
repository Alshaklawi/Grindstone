#!/usr/bin/env bash
set -euo pipefail

source_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
runtime="${1:-$source_root/build/plugin-hub-v5}"
mkdir -p "$runtime"
runtime="$(cd "$runtime" && pwd)"
if [[ "$runtime" == "$source_root" ]]; then
    echo 'The packager runtime must be separate from the plugin source.' >&2
    exit 1
fi
cd "$runtime"

if [[ ! -f package.jar ]]; then
    curl --location --fail --retry 4 --max-time 180 --output bundle.tar.zst https://github.com/runelite/plugin-hub-tooling/releases/download/v5/bundle.tar.zst
    echo '5d206634b17c92fcf78c11b871b602832822e2b6d3d199caff734cae8ef6b3f1  bundle.tar.zst' | sha256sum --check
    tar -xf bundle.tar.zst
fi
if [[ ! -d plugin-hub/.git ]]; then
    git clone --depth 1 https://github.com/runelite/plugin-hub.git plugin-hub
fi

commit="$(git -C "$source_root" rev-parse HEAD)"
repository="$(git -C "$source_root" remote get-url origin)"
if [[ -n "${GITHUB_REPOSITORY:-}" ]]; then
    repository="https://github.com/$GITHUB_REPOSITORY.git"
fi
printf 'repository=%s\ncommit=%s\n' "$repository" "$commit" > plugin-hub/plugins/grindstone
git -C plugin-hub add plugins/grindstone
git -C plugin-hub -c user.name='Plugin verification' -c user.email='verification@localhost' commit --allow-empty -m 'Verify local Grindstone checkout'

# The descriptor keeps its real GitHub URL. Only this process redirects cloning
# to the exact checkout, including pull-request merge commits and private repos.
export GIT_CONFIG_COUNT=2
export GIT_CONFIG_KEY_0="url.file://$source_root/.insteadOf"
export GIT_CONFIG_VALUE_0="$repository"
export GIT_CONFIG_KEY_1=protocol.file.allow
export GIT_CONFIG_VALUE_1=always

mkdir -p api
chmod +x prepare.sh gradle/bin/gradle
runelite_version="$(tr -d '\r\n' < plugin-hub/runelite.version)"
if [[ ! -f "api/$runelite_version.api" ]]; then
    ./prepare.sh
fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
previous_artifacts="$(mktemp -d "$runtime/previous-artifacts.XXXXXX")"
for extension in jar log zip; do
    artifact="/tmp/jars/grindstone.$extension"
    if [[ -f "$artifact" ]]; then
        mv -- "$artifact" "$previous_artifacts/"
    fi
done
touch packager-start
PACKAGE_IS_PR=true FORCE_BUILD=grindstone "$java_bin" -Djava.awt.headless=true -XX:+UseParallelGC -cp package.jar net.runelite.pluginhub.packager.Packager 2>&1 | tee packager.log

python3 - <<'PY'
import json
from pathlib import Path

manifest = Path('/tmp/manifest_diff')
if not manifest.exists() or manifest.stat().st_mtime < Path('packager-start').stat().st_mtime:
    raise SystemExit('Packager did not produce a fresh verification result')
result = json.loads(manifest.read_text())
if not any(entry.get('internalName') == 'grindstone' for entry in result.get('addJarData', [])):
    raise SystemExit('Packager did not verify a Grindstone artifact; inspect packager.log')
print('Plugin Hub v5 Linux packaging and API verification passed')
PY
