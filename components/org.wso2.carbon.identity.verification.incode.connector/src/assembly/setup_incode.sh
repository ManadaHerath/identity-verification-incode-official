#!/bin/bash
# Deploys the Incode connector into a WSO2 Identity Server distribution.
# Extract this archive inside <IS_HOME>, change into the extracted directory and run this script.
# Restart WSO2 Identity Server afterwards: connection templates are read at startup.

set -e

IS_HOME="$(cd .. && pwd)"
WEBAPPS="$IS_HOME/repository/deployment/server/webapps"

if [ ! -d "$IS_HOME/repository/components/dropins" ]; then
    echo "Run this script from the extracted directory inside <IS_HOME>." >&2
    exit 1
fi

cp ./dropins/*.jar "$IS_HOME/repository/components/dropins/"
mkdir -p "$IS_HOME/repository/resources/identity/extensions/connections/"
cp -r ./connections/incode-idv "$IS_HOME/repository/resources/identity/extensions/connections/"

for dir in console/resources/connections/assets/images/logos \
           console/resources/assets/images/icons \
           myaccount/resources/connections/assets/images/logos \
           authenticationendpoint/libs/themes/default/assets/images/identity-providers; do
    mkdir -p "$WEBAPPS/$dir"
    cp ./images/incode.svg "$WEBAPPS/$dir/"
done

echo "Incode connector deployed. Restart WSO2 Identity Server."
