#! /bin/bash

function createGpgDirIfNeeded() {
    [ ! -d "$1" ] && \
        mkdir -p "$1" && \
        chown -R "$USER:$(id -gn)" "$1" && \
        chmod 700 "$1"
}

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_f13b410addf6_key" = "x" ]; then
    echo "Skipping deployment setup since we are building a pull request"
else
    mkdir -p .build .build_source

    openssl aes-256-cbc \
        -K "$encrypted_f13b410addf6_key" \
        -iv "$encrypted_f13b410addf6_iv" \
        -in .travis/encrypted.tar.gz.enc \
        -out .build_source/encrypted.tar.gz -d

    tar xf .build_source/encrypted.tar.gz -C .build

    createGpgDirIfNeeded "$GPG_DIR"

    cp -v .build/gpg/travis.* "$GPG_DIR/"

    if [ "$GPG_IMPORT_PRIVATE_KEY" = "true" ]; then
        gpg --import --no-tty --batch --yes "$GPG_DIR/travis.priv.bin"
    fi
fi
