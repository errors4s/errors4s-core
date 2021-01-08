#!/usr/bin/env bash

set -e

sbt docs/mdoc &>/dev/null
diff -s ./README.md errors4s-docs/target/mdoc/README.md &>/dev/null
