#!/usr/bin/env bash

if [ $# != 2 ]; then exit 1; fi

enc_key=$1
enc_iv=$2

openssl aes-256-cbc -K ${enc_key} -iv ${enc_iv} -in .travis.tar.gz.enc -out .travis.tar.gz -d
eval "$(ssh-agent -s)"
ssh-add travis/deploy_key
git config user.name "Matt Sicker"
git config user.email "mattsicker@apache.org"
git checkout ${TRAVIS_BRANCH}
./sbtx "release with-defaults"
