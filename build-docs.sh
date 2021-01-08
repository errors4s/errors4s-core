#!/usr/bin/env bash

set -e

sbt docs/mdoc
cp errors4s-docs/target/mdoc/README.md ./README.md
./check-docs.sh
