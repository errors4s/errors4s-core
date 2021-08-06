#!/usr/bin/env bash

set -e

source sbt-shell-release/src/bash/functions.sh

export SBT_TASKS=';scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll;+test;+test:doc;docs/mdoc --check'
readonly SBT_TASKS

sbt_release
