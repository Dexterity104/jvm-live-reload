set shell := ["bash", "-c"]

gradle := "./gradlew"
mill := "mill"
sbt := "sbt"

default: publish-local-sbt

[doc('Clears $HOME/.ivy2/local/me.seroperson/ and $HOME/.m2/repository/me/seroperson/.')]
clear-local-repos:
  rm -rf $HOME/.ivy2/local/me.seroperson/ $HOME/.m2/repository/me/seroperson

[doc('Creates version.txt with current version according to git.')]
calculate-version:
  if [ ! -e "version.txt" ]; then \
    {{ sbt }} catVersion; \
  else \
    true; \
  fi

[private]
is-release: calculate-version
  [[ `cat version.txt` != *"SNAPSHOT"* ]]

test-sbt: publish-local-if-unpublished-sbt
  {{ sbt }} quickScripted

[private]
publish-local-if-unpublished-sbt:
  if [ ! -e "$HOME/.ivy2/local/me.seroperson/jvm-live-reload-build-link/" ]; then \
    {{ sbt }} quickLocalPublish; \
  else \
    true; \
  fi

publish-local-sbt:
  {{ sbt }} quickLocalPublish

publish-sbt:
  {{ sbt }} ci-release

test-gradle:
  {{ gradle }} :gradle:check

publish-gradle: is-release
  {{ gradle }} :gradle:publishPlugins \
    -Pgradle.publish.key=$GRADLE_PUBLISH_KEY \
    -Pgradle.publish.secret=$GRADLE_SECRET_KEY

test-mill: publish-local-mill
  {{ mill }} mill-live-reload.integration.testLocal

publish-local-mill: calculate-version
  {{ mill }} mill-live-reload.publishLocal

publish-mill: calculate-version
  {{ mill }} mill-live-reload.publishSonatypeCentral

[private]
code-format-check-sbt:
  {{ sbt }} fmtCheckAll

[private]
code-format-apply-sbt:
  {{ sbt }} fmtAll

[private]
code-format-check-mill:
  {{ mill }} mill-live-reload.checkFormat

[private]
code-format-apply-mill:
  {{ mill }} mill-live-reload.reformat

[private]
code-format-check-gradle:
  {{ gradle }} spotlessCheck

[private]
code-format-apply-gradle:
  {{ gradle }} spotlessApply

[doc('Checks formatting. Fails if formatting does not match style.')]
code-format-check-all: code-format-check-sbt code-format-check-mill code-format-check-gradle
  @echo "SUCCESS"

[doc('Applies formatting.')]
code-format-apply-all: code-format-apply-sbt code-format-apply-mill code-format-apply-gradle
  @echo "SUCCESS"

[doc('Runs all tests.')]
test-all: test-sbt test-mill test-gradle
  @echo "SUCCESS"

