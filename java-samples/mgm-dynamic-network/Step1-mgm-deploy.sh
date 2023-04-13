#!/bin/sh

echo "---Set Env---"
RPC_HOST=localhost
RPC_PORT=8888
P2P_GATEWAY_HOST=localhost
P2P_GATEWAY_PORT=8080
API_URL="https://$RPC_HOST:$RPC_PORT/api/v1"
WORK_DIR=~/Corda/corda5/corda5-samples/java-samples/mgm-dynamic-network/register-mgm
mkdir -p "$WORK_DIR"
RUNTIME_OS=~/Corda/corda5/corda-runtime-os

echo "\n---Create a mock CA and signing keys---"
cd "$WORK_DIR"
#default signing key
echo '-----BEGIN CERTIFICATE-----
MIIB7zCCAZOgAwIBAgIEFyV7dzAMBggqhkjOPQQDAgUAMFsxCzAJBgNVBAYTAkdC
MQ8wDQYDVQQHDAZMb25kb24xDjAMBgNVBAoMBUNvcmRhMQswCQYDVQQLDAJSMzEe
MBwGA1UEAwwVQ29yZGEgRGV2IENvZGUgU2lnbmVyMB4XDTIwMDYyNTE4NTI1NFoX
DTMwMDYyMzE4NTI1NFowWzELMAkGA1UEBhMCR0IxDzANBgNVBAcTBkxvbmRvbjEO
MAwGA1UEChMFQ29yZGExCzAJBgNVBAsTAlIzMR4wHAYDVQQDExVDb3JkYSBEZXYg
Q29kZSBTaWduZXIwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQDjSJtzQ+ldDFt
pHiqdSJebOGPZcvZbmC/PIJRsZZUF1bl3PfMqyG3EmAe0CeFAfLzPQtf2qTAnmJj
lGTkkQhxo0MwQTATBgNVHSUEDDAKBggrBgEFBQcDAzALBgNVHQ8EBAMCB4AwHQYD
VR0OBBYEFLMkL2nlYRLvgZZq7GIIqbe4df4pMAwGCCqGSM49BAMCBQADSAAwRQIh
ALB0ipx6EplT1fbUKqgc7rjH+pV1RQ4oKF+TkfjPdxnAAiArBdAI15uI70wf+xlL
zU+Rc5yMtcOY4/moZUq36r0Ilg==
-----END CERTIFICATE-----' > ./gradle-plugin-default-key.pem

#Generate a signing key:
keytool -genkeypair -alias "signing key 1" -keystore signingkeys.pfx -storepass "keystore password" -dname "cn=CPI Plugin Example - Signing Key 1, o=R3, L=London, c=GB" -keyalg RSA -storetype pkcs12 -validity 4000
#Import gradle-plugin-default-key.pem into the keystore
keytool -importcert -keystore signingkeys.pfx -storepass "keystore password" -noprompt -alias gradle-plugin-default-key -file gradle-plugin-default-key.pem
#cd "$RUNTIME_OS"
#./gradlew :applications:tools:p2p-test:fake-ca:clean :applications:tools:p2p-test:fake-ca:appJar
#java -jar ./applications/tools/p2p-test/fake-ca/build/bin/corda-fake-ca-5.0.0.0-Gecko-SNAPSHOT.jar -m /tmp/ca -a RSA -s 3072 ca
cd "$WORK_DIR"


echo "\n---Build mgm CPB---"
#cd "$RUNTIME_OS"
#./gradlew testing:cpbs:mgm:build
#cp testing/cpbs/mgm/build/libs/mgm-5.0.0.0-Gecko-SNAPSHOT-package.cpb "$WORK_DIR"
echo '{
  "fileFormatVersion" : 1,
  "groupId" : "CREATE_ID",
  "registrationProtocol" :"net.corda.membership.impl.registration.dynamic.mgm.MGMRegistrationService",
  "synchronisationProtocol": "net.corda.membership.impl.synchronisation.MgmSynchronisationServiceImpl"
}' > "$WORK_DIR"/MgmGroupPolicy.json
cd "$WORK_DIR"
mv ./mgm-5.0.0.0-Gecko-SNAPSHOT-package.cpb mgm.cpb


echo "\n---Build and upload MGM CPI---"
cd "$WORK_DIR"
#Run this command to turn a CPB into a CPI
sh ~/.corda/cli/corda-cli.sh package create-cpi --cpb mgm.cpb --group-policy MgmGroupPolicy.json --cpi-name "mgm cpi" --cpi-version "1.0.0.0-SNAPSHOT" --file mgm.cpi --keystore signingkeys.pfx --storepass "keystore password" --key "signing key 1"
#Import the gradle plugin default key into Corda
curl --insecure -u admin:admin -X PUT -F alias="gradle-plugin-default-key" -F certificate=@gradle-plugin-default-key.pem https://localhost:8888/api/v1/certificates/cluster/code-signer
#Export the signing key certificate from the key store
keytool -exportcert -rfc -alias "signing key 1" -keystore signingkeys.pfx -storepass "keystore password" -file signingkey1.pem
#Import the signing key into Corda
curl --insecure -u admin:admin -X PUT -F alias="signingkey1-2022" -F certificate=@signingkey1.pem https://localhost:8888/api/v1/certificates/cluster/code-signer
CPI_PATH=./mgm.cpi
curl --insecure -u admin:admin -F upload=@$CPI_PATH $API_URL/cpi/
echo "\n"
read -p "Enter the CPI_ID from the returned body:" CPI_ID
echo "CPI_ID:" $CPI_ID


echo "---Create MGM VNode---"
curl --insecure -u admin:admin $API_URL/cpi/status/$CPI_ID
echo "\n"
read -p "Enter the CPI_CHECKSUM from the returned body:" CPI_CHECKSUM
curl --insecure -u admin:admin -d '{ "request": {"cpiFileChecksum": "'$CPI_CHECKSUM'", "x500Name": "C=GB, L=London, O=MGM"}}' $API_URL/virtualnode
echo "\n"
read -p "Enter the MGM_HOLDING_ID from the returned body:" MGM_HOLDING_ID
echo "MGM_HOLDING_ID:" $MGM_HOLDING_ID


echo "---Assign soft HSM---"
curl --insecure -u admin:admin -X POST $API_URL/hsm/soft/$MGM_HOLDING_ID/SESSION_INIT
echo "\n"
curl --insecure -u admin:admin -X POST $API_URL/keys/$MGM_HOLDING_ID/alias/$MGM_HOLDING_ID-session/category/SESSION_INIT/scheme/CORDA.ECDSA.SECP256R1
echo "\n"
read -p "Enter the SESSION_KEY_ID from the returned body:" SESSION_KEY_ID
echo "SESSION_KEY_ID:" $SESSION_KEY_ID
curl --insecure -u admin:admin -X POST $API_URL/hsm/soft/$MGM_HOLDING_ID/PRE_AUTH
echo "\nECDH_KEY_ID: "
curl --insecure -u admin:admin -X POST $API_URL/keys/$MGM_HOLDING_ID/alias/$MGM_HOLDING_ID-auth/category/PRE_AUTH/scheme/CORDA.ECDSA.SECP256R1
echo "\n"
read -p "Enter the ECDH_KEY_ID from the returned body:" ECDH_KEY_ID
echo "ECDH_KEY_ID:" $ECDH_KEY_ID


echo "\n--Set up the TLS key pair and certificate---"
curl -k -u admin:admin -X POST -H "Content-Type: application/json" $API_URL/keys/p2p/alias/p2p-TLS/category/TLS/scheme/CORDA.RSA
echo "\n"
read -p "Enter the TLS_KEY_ID from the returned body:" TLS_KEY_ID
echo "TLS_KEY_ID:" $TLS_KEY_ID
curl -k -u admin:admin  -X POST -H "Content-Type: application/json" -d '{"x500Name": "CN=CordaOperator, C=GB, L=London, O=Org", "subjectAlternativeNames": ["'$P2P_GATEWAY_HOST'"]}' $API_URL"/certificates/p2p/"$TLS_KEY_ID > "$WORK_DIR"/request1.csr
read -p "Wait for download to be finished, Then press any key to continue..." ANY
cd "$RUNTIME_OS"
java -jar ./applications/tools/p2p-test/fake-ca/build/bin/corda-fake-ca-5.0.0.0-Gecko-SNAPSHOT.jar -m /tmp/ca csr "$WORK_DIR"/request1.csr
cd "$WORK_DIR"
curl -k -u admin:admin -X PUT  -F certificate=@/tmp/ca/request1/certificate.pem -F alias=p2p-tls-cert $API_URL/certificates/cluster/p2p-tls


echo "---Disable revocation checks---"
curl --insecure -u admin:admin -X GET $API_URL/config/corda.p2p.gateway
echo "\n"
read -p "Enter the CONFIG_VERSION from the returned body:" CONFIG_VERSION
echo "CONFIG_VERSION:" $CONFIG_VERSION
curl -k -u admin:admin -X PUT -d '{"section":"corda.p2p.gateway", "version":"'$CONFIG_VERSION'", "config":"{ \"sslConfig\": { \"revocationCheck\": { \"mode\": \"OFF\" }  }  }", "schemaVersion": {"major": 1, "minor": 0}}' $API_URL"/config"


echo "\n---Register MGM---"
TLS_CA_CERT=$(cat /tmp/ca/ca/root-certificate.pem | awk '{printf "%s\\n", $0}')
REGISTRATION_CONTEXT='{
  "corda.session.key.id": "'$SESSION_KEY_ID'",
  "corda.ecdh.key.id": "'$ECDH_KEY_ID'",
  "corda.group.protocol.registration": "net.corda.membership.impl.registration.dynamic.member.DynamicMemberRegistrationService",
  "corda.group.protocol.synchronisation": "net.corda.membership.impl.synchronisation.MemberSynchronisationServiceImpl",
  "corda.group.protocol.p2p.mode": "Authenticated_Encryption",
  "corda.group.key.session.policy": "Combined",
  "corda.group.pki.session": "NoPKI",
  "corda.group.pki.tls": "Standard",
  "corda.group.tls.version": "1.3",
  "corda.endpoints.0.connectionURL": "https://'$P2P_GATEWAY_HOST':'$P2P_GATEWAY_PORT'",
  "corda.endpoints.0.protocolVersion": "1",
  "corda.group.truststore.tls.0" : "'$TLS_CA_CERT'"
}'
REGISTRATION_REQUEST='{"memberRegistrationRequest":{"action": "requestJoin", "context": '$REGISTRATION_CONTEXT'}}'
curl --insecure -u admin:admin -d "$REGISTRATION_REQUEST" $API_URL/membership/$MGM_HOLDING_ID
echo "\n"
read -p "Enter the REGISTRATION_ID from the returned body:" REGISTRATION_ID
echo "REGISTRATION_ID:" $REGISTRATION_ID
curl --insecure -u admin:admin -X GET $API_URL/membership/$MGM_HOLDING_ID/$REGISTRATION_ID
echo "\n"


echo "---Configure virtual node as network participant---"
curl -k -u admin:admin -X PUT -d '{"p2pTlsCertificateChainAlias": "p2p-tls-cert", "useClusterLevelTlsCertificateAndKey": true, "sessionKeyId": "'$SESSION_KEY_ID'"}' $API_URL/network/setup/$MGM_HOLDING_ID
echo "\n"


echo "---Export group policy for group---"
cd "$WORK_DIR"
mkdir -p "./register-member"
curl --insecure -u admin:admin -X GET $API_URL/mgm/$MGM_HOLDING_ID/info > "$WORK_DIR/register-member/GroupPolicy.json"
