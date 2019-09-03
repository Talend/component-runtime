#! /bin/bash

cache_dir="${CACHE_DIR:-$HOME/build/Talend/component-runtime}" &&
  mkdir -p "$cache_dir" &&
  curl -sSL https://download.sourceclear.com/ci.sh | CACHE_DIR="${cache_dir}/.sourceclear" bash
