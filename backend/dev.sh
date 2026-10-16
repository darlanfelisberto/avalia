#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$script_dir/.env.local" ]]; then
  set -a
  source "$script_dir/.env.local"
  set +a
fi

cd "$script_dir"
exec mvn clean liberty:dev
