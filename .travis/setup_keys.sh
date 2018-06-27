#! /bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    echo "Skipping deployment setup since we are building a pull request"
else
    mkdir -p ~/.build/ ~/.build_source/
    openssl aes-256-cbc -K $encrypted_f13b410addf6_key -iv $encrypted_f13b410addf6_iv -in .travis/encrypted.tar.gz.enc -out ~/.build_source/encrypted.tar.gz -d
    tar xvf ~/.build_source/encrypted.tar.gz -C ~/.build/
    mv ~/.build/docker ~/.docker/
    mv ~/.build/gpg ~/.gpg/
fi
