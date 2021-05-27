#!/usr/bin/env bash

set -e

if git update-index --refresh && git diff-index --quiet @ --
then
    sbt '+publishLocal'
    sbt ';docs/clean;docs/mdoc'
    if git update-index --refresh && git diff-index --quiet @ --
    then
        echo 'Generated documents under VCS control are up to date.'
        exit 0
    else
        echo 'Generated documents under VCS control are not up to date.' 1>&2
        git status 1>&2
        exit 2
    fi
else
    echo 'There are local changes. The docs-check.sh may only be executed in a clean git environment.' 1>&2
    git status
    exit 1
fi
