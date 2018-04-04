#!/usr/bin/env bash -e

if [[ $# != 2 ]]; then exit 1; fi
enc_key=$1
enc_iv=$2

committer_email="$(git log --pretty=format:%ce -1)"
deploy_email="jvz@travis-ci.org"
if [[ ${committer_email} == ${deploy_email} ]]; then
  echo Ignoring CI commit.
  exit 0
fi

openssl aes-256-cbc -K ${enc_key} -iv ${enc_iv} -in .travis.tar.gz.enc -out .travis.tar.gz -d
tar -xzf .travis.tar.gz
eval "$(ssh-agent -s)"
ssh-add travis/deploy_key
git config user.name "Matt Sicker"
git config user.email ${deploy_email}
git remote set-url origin git@github.com:jvz/sbt-rat.git
git checkout ${TRAVIS_BRANCH}
./sbtx "release with-defaults"
