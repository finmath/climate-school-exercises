#!/bin/bash

set -euo pipefail

target_directory="${1:?usage: prepare-macos-dependencies.sh TARGET_DIRECTORY}"

if [[ "$(uname -s)" != "Darwin" || "$(uname -m)" != "arm64" ]]; then
	echo "The signed macOS package is intentionally built for Apple Silicon (arm64)." >&2
	exit 1
fi

if [[ ! -d "$target_directory" ]]; then
	echo "Dependency directory does not exist: $target_directory" >&2
	exit 1
fi

shopt -s nullglob
jblas_archives=("$target_directory"/jblas-*.jar)
gluegen_archives=("$target_directory"/gluegen-rt-*-natives-macosx-universal.jar)

if (( ${#jblas_archives[@]} != 1 )); then
	echo "Expected exactly one jblas JAR, found ${#jblas_archives[@]}." >&2
	exit 1
fi

if (( ${#gluegen_archives[@]} != 1 )); then
	echo "Expected exactly one macOS GlueGen native JAR, found ${#gluegen_archives[@]}." >&2
	exit 1
fi

remove_entry() {
	local archive="$1"
	local entry="$2"

	if ! unzip -Z1 "$archive" | grep -Fx "$entry" >/dev/null; then
		echo "Native library is already absent from $(basename "$archive"): $entry"
		return
	fi

	zip -q -d "$archive" "$entry"

	if unzip -Z1 "$archive" | grep -Fx "$entry" >/dev/null; then
		echo "Failed to remove $entry from $archive" >&2
		exit 1
	fi
}

# These dependency releases contain only Intel macOS binaries. They cannot be
# loaded by the Apple Silicon JVM bundled in this application. Apple still
# inspects them while notarizing the enclosing JAR, so remove those unusable
# payloads before jpackage seals and signs the application bundle.
remove_entry "${jblas_archives[0]}" \
	"lib/static/Mac OS X/x86_64/libjblas_arch_flavor.jnilib"
remove_entry "${jblas_archives[0]}" \
	"lib/static/Mac OS X/x86_64/sse3/libjblas.jnilib"
remove_entry "${gluegen_archives[0]}" \
	"natives/macosx-universal/libgluegen-rt.jnilib"

echo "Confirmed Intel-only native libraries are absent from Apple Silicon package dependencies."
