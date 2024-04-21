#!/bin/bash

set -o nounset

readonly IMAGE=mini-ruccola-scala:1

cmd_build() {
  docker build \
    --build-arg ARG_USER=$USER \
    --build-arg ARG_GROUP=$(id -gn) \
    --progress=plain \
    -t $IMAGE .
}

cmd_run() {
  docker run --rm -it \
    -v "$(pwd):/home/${USER}/work" \
    $IMAGE "$@"
}

cmd="$1"; shift
case $cmd in
  build | b* )
    cmd_build "$@"
;; run | r* )
     cmd_run "$@"
;; * )
     echo "invalid command (${cmd})" >&2
     ;;
esac
