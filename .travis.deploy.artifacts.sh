PROJECT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -B | grep -v '\[')
if [[ "$PROJECT_VERSION" =~ .*SNAPSHOT ]] && [[ "${TRAVIS_BRANCH}" = "master" ]] && [[ "${TRAVIS_PULL_REQUEST}" = "false" ]];
then
  mvn deploy -DskipTests -B;
fi