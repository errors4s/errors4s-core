#!/usr/bin/env bash

set -e

source sbt-release-script/src/bash/functions.sh

export SBT_TASKS='++2.13.6;scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll;++3.0.0;+test;+test:doc;'
readonly SBT_TASKS

sbt_release
