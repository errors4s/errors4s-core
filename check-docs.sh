#!/usr/bin/env bash

set -ex

sbt docs/mdoc &>/dev/null
if diff -s ./README.md errors4s-docs/target/mdoc/README.md
then
    exit 0
else
    diff ./README.md errors4s-docs/target/mdoc/README.md
    exit 1
fi
