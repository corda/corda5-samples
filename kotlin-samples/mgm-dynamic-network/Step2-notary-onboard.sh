#!/bin/sh

echo "---Set Env---"
RPC_HOST=localhost
RPC_PORT=8888
P2P_GATEWAY_HOST=localhost
P2P_GATEWAY_PORT=8080
API_URL="https://$RPC_HOST:$RPC_PORT/api/v1"
WORK_DIR=~/Corda/corda5/corda5-samples/kotlin-samples/mgm-dynamic-network/register-mgm
RUNTIME_OS=~/Corda/corda5/corda-runtime-os


echo "\n---Build and upload Notary CPI---"
cd $WORK_DIR
cp ./notary.cpb ./register-member
cd "$WORK_DIR/register-member/"
##Run this command to turn a CPB into a CPI
sh ~/.corda/cli/corda-cli.sh package create-cpi --cpb notary.cpb --group-policy GroupPolicy.json --cpi-name "notary cpi" --cpi-version "1.0.0.0-SNAPSHOT" --file notary.cpi --keystore ../signingkeys.pfx --storepass "keystore password" --key "signing key 1"
CPI_PATH="$WORK_DIR/register-member/notary.cpi"
curl --insecure -u admin:admin -F upload=@$CPI_PATH $API_URL/cpi/
echo "\n"
read -p "Enter the Notary CPI_ID from the returned body:" CPI_ID
echo "CPI_ID:" $CPI_ID
curl --insecure -u admin:admin $API_URL/cpi/status/$CPI_ID
echo "\n"
read -p "Enter the CPI_CHECKSUM from the returned body:" CPI_CHECKSUM


echo "\n---Create a Member virtual node---"
echo "\n"
read -p "Enter the X500_NAME from the returned body (Formatt: C=GB,L=London,O=NotaryRep1):" X500_NAME
curl --insecure -u admin:admin -d '{"request": {"cpiFileChecksum": "'$CPI_CHECKSUM'", "x500Name": "'$X500_NAME'"}}' $API_URL/virtualnode
echo "\n"
read -p "Enter the HOLDING_ID from the returned body:" HOLDING_ID
echo "HOLDING_ID:" $HOLDING_ID
echo "\n"


echo "---Assign soft HSM---"
curl --insecure -u admin:admin -X POST $API_URL/hsm/soft/$HOLDING_ID/SESSION_INIT
echo "\n"
curl --insecure -u admin:admin -X POST $API_URL'/keys/'$HOLDING_ID'/alias/'$HOLDING_ID'-session/category/SESSION_INIT/scheme/CORDA.ECDSA.SECP256R1'
echo "\n"
read -p "Enter the SESSION_KEY_ID from the returned body:" SESSION_KEY_ID
echo "SESSION_KEY_ID:" $SESSION_KEY_ID
curl --insecure -u admin:admin -X POST $API_URL/hsm/soft/$HOLDING_ID/LEDGER
echo "\n"
curl --insecure -u admin:admin -X POST $API_URL/keys/$HOLDING_ID/alias/$HOLDING_ID-ledger/category/LEDGER/scheme/CORDA.ECDSA.SECP256R1
echo "\n"
read -p "Enter the LEDGER_KEY_ID from the returned body:" LEDGER_KEY_ID
echo "LEDGER_KEY_ID:" $LEDGER_KEY_ID
curl --insecure -u admin:admin -X POST $API_URL/hsm/soft/$HOLDING_ID/NOTARY
echo "\n"
curl --insecure -u admin:admin -X POST $API_URL/keys/$HOLDING_ID/alias/$HOLDING_ID-notary/category/NOTARY/scheme/CORDA.ECDSA.SECP256R1
echo "\n"
read -p "Enter the NOTARY_KEY_ID from the returned body:" NOTARY_KEY_ID
echo "NOTARY_KEY_ID:" $NOTARY_KEY_ID


echo "\n---Configure virtual node as network participant---"
curl -k -u admin:admin -X PUT -d '{"p2pTlsCertificateChainAlias": "p2p-tls-cert", "useClusterLevelTlsCertificateAndKey": true, "sessionKeyId": "'$SESSION_KEY_ID'"}' $API_URL/network/setup/$HOLDING_ID
echo "\n"


echo "\n---Build Notary registration context---"
echo "\n"
read -p "Enter the NOTARY_SERVICE_NAME (Formatt: C=GB,L=London,O=NotaryServiceA):" NOTARY_SERVICE_NAME
echo "NOTARY_SERVICE_NAME:" $NOTARY_SERVICE_NAME
REGISTRATION_CONTEXT='{
  "corda.session.key.id": "'$SESSION_KEY_ID'",
  "corda.session.key.signature.spec": "SHA256withECDSA",
  "corda.ledger.keys.0.id": "'$LEDGER_KEY_ID'",
  "corda.ledger.keys.0.signature.spec": "SHA256withECDSA",
  "corda.notary.keys.0.id": "'$NOTARY_KEY_ID'",
  "corda.notary.keys.0.signature.spec": "SHA256withECDSA",
  "corda.endpoints.0.connectionURL": "https://'$P2P_GATEWAY_HOST':'$P2P_GATEWAY_PORT'",
  "corda.endpoints.0.protocolVersion": "1",
  "corda.roles.0" : "notary",
  "corda.notary.service.name" : "'$NOTARY_SERVICE_NAME'",
  "corda.notary.service.plugin" : "net.corda.notary.NonValidatingNotary"
}'
REGISTRATION_REQUEST='{"memberRegistrationRequest":{"action": "requestJoin", "context": '$REGISTRATION_CONTEXT'}}'


echo "\n---Register Notary VNode---"
curl --insecure -u admin:admin -d "$REGISTRATION_REQUEST" $API_URL/membership/$HOLDING_ID
echo "\n"
read -p "Enter the REGISTRATION_ID from the returned body:" REGISTRATION_ID
echo "REGISTRATION_ID:" $REGISTRATION_ID
curl --insecure -u admin:admin -X GET $API_URL/membership/$HOLDING_ID/$REGISTRATION_ID
echo "\n"
curl --insecure -u admin:admin -X GET $API_URL/members/$HOLDING_ID
