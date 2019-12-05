#!/usr/bin/env bash

# Retrires a maven build until it succesdes or until number of attempts reaches 3, whatever comes first.
# This is a temporary hack until issues with jpms have been fixed.

ATTEMPTS=0

mvn install -P ssb-bip dependency:copy-dependencies -DincludeScope=runtime -Dmaven.repo.local=/drone/src/.m2/repository -Dmaven.javadoc.skip=true -DskipTests --batch-mode --global-settings settings.xml
until [ "$?" -ne 1 ] || [ "$ATTEMPTS" -ge 2 ]; do
  ((ATTEMPTS++))
  sleep 1
  printf "Failed attempts: %s\n" "$ATTEMPTS"
  printf "Retrying...\n"
  mvn install -P ssb-bip dependency:copy-dependencies -DincludeScope=runtime -Dmaven.repo.local=/drone/src/.m2/repository -Dmaven.javadoc.skip=true -DskipTests --batch-mode --global-settings settings.xml
done
