#!/usr/bin/env bash

set -ex

PUBLISH='NO'
declare FAKE_HOME
declare DRY_RUN
# Set Default Environment Variables #

# DRY_RUN
if [ -z "$DRY_RUN" ]
then
    DRY_RUN=0
fi

readonly DRY_RUN

# FAKE_HOME
if [ -z "$FAKE_HOME" ]
then
    FAKE_HOME="$(mktemp -d)"
    # Register cleanup trap on exit
    trap 'rm -rf ${FAKE_HOME}' EXIT

    # Copy local caches into fake home to speed things up.
    declare -a CACHE_DIRS=('.sbt' '.ivy2/cache' '.coursier/cache/v1' '.cache/coursier/v1')

    for d in ${CACHE_DIRS[*]}
    do
        TARGET_PATH="${FAKE_HOME}/${d}"
        ORIGIN_PATH="${HOME}/${d}"

        if [ -d "$ORIGIN_PATH" ]
        then
            mkdir -vp "$TARGET_PATH"
            cp -bRv "${ORIGIN_PATH}/"* "$TARGET_PATH"
        else
            continue
        fi
    done
elif [ ! -d "$FAKE_HOME" ]
then
    echo "$FAKE_HOME is not a directory, but the FAKE_HOME value must be a directory." 1>&2
    exit 2
fi

readonly FAKE_HOME
export HOME="${FAKE_HOME}"

# JVM Options #
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Duser.home=${FAKE_HOME}"

# Release #

if git update-index --refresh && git diff-index --quiet @ -- || [ "$DRY_RUN" -ne 0 ]
then
    # Publish locally first for the scripted tests.
    sbt '+clean'
    sbt '+publishLocal'
    sbt +';scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll;test;test:doc;'

    # Exit here on DRY_RUN
    if [ "$DRY_RUN" -ne 0 ]
    then
        exit 0
    fi

    read -r -p 'Continue with publish? Type (YES): ' PUBLISH
    if [ "${PUBLISH:?}" = 'YES' ]
    then
        sbt '+publishSigned'
    else
        echo "${PUBLISH} is not YES. Aborting." 1>&2
    fi
else
    if [ "$DRY_RUN" -eq 0 ]
    then
        echo 'Uncommited local changes. Aborting' 1>&2
        exit 1
    else
        echo 'Dry run complete'
        exit 0
    fi
fi
