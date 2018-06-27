#! /bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    echo "Skipping deployment setup since we are building a pull request"
else
    mkdir -p ~/.build/
    openssl aes-256-cbc -K $encrypted_f13b410addf6_key -iv $encrypted_f13b410addf6_iv -in encrypted.tar.gz.enc -out encrypted.tar.gz -d
    tar xvf ~/.gpg/gpg.tar.gz -C ~/.build/
    mv ~/.build/docker ~/.docker/
    mv ~/.build/gpg ~/.gpg/
fi
