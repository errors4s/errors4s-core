#!/usr/bin/env bash

set -e

declare RELEASE_VERSION
declare NEXT_VERSION

function check_for_snapshots() {
    if sbt 'show libraryDependencies' |& grep -q -i 'snapshot'
    then
        echo 'Refusing to release with snapshot dependencies.'
        return 1
    else
        return 0
    fi
}

function check_build() {
    sbt ';+clean;+versionSchemeEnforcerCheck;+test;+doc;+test:doc'
    ./check-docs.sh
}

function set_release_version() {
    if [ -n "$RELEASE_VERSION" ]
    then
        echo "version in ThisBuild := \"${RELEASE_VERSION}\"\n" > version.sbt
        check_build
    else
        echo 'RELEASE_VERSION is not set.'
        return 1
    fi
}

function set_next_version() {
    if [ -n "$NEXT_VERSION" ]
    then
        echo "version in ThisBuild := \"${NEXT_VERSION}\"\n" > version.sbt
        check_build
    else
        echo 'NEXT_VERSION is not set.'
        return 1
    fi
}

function commit_release {
    set_release_version
    git add version.sbt
    git commit -m "Release $RELEASE_VERSION"
    git tag -s "$RELEASE_VERSION" HEAD
}

function commit_next {
    set_next_version
    git add version.sbt
    git commit -m "Release $NEXT_VERSION"
}

function set_version() {
    local RESPONSE=''
    RELEASE_VERSION="$(sed 's/\-SNAPSHOT//' version.sbt | grep -o '\"[^\"]\+\"' | tr -d '"')"
    printf "Version will be: %s\n" "$RELEASE_VERSION"
    read -rp 'Is this version okay (type "YES"): ' RESPONSE
    if [ "$RESPONSE" = "YES" ]
    then
        # Validate current and next versions
        set_release_version
        read -rp "Next version: " NEXT_VERSION
        set_next_version

        # Reset release version and make commits
        commit_release
        commit_next
        git checkout "$RELEASE_VERSION"
        git push
        git push origin "$RELEASE_VERSION"
    else
        echo "\"$RESPONSE\" is not \"YES\". Aborting."
        return 1
    fi
}

function main() {
    if [ "$(git --no-pager status -s | wc -l)" = "0" ]
    then
        check_for_snapshots
        set_version
    else
        echo 'Uncommitted local changes.'
        git --no-pager status
        return 1
    fi
}

main "$@"
