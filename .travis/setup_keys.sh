#! /bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "true" ] || [ "x$encrypted_03e441b90173_key" = "x" ]; then
    echo "Skipping deployment setup since we are building a pull request"
else
    mkdir -p ~/.gpg/
    openssl aes-256-cbc -K $encrypted_03e441b90173_key -iv $encrypted_03e441b90173_iv -in .travis/gpg/gpg.tar.enc -out ~/.gpg/gpg.tar -d
    tar xvf ~/.gpg/gpg.tar.gz -C ~/.gpg/
fi
