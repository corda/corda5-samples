#!/bin/sh
rm -rf ./workspace
rm -rf ./logs

echo "---Clean Up Env---"
WORK_DIR=./register-mgm
cd $WORK_DIR

rm signingkey1.pem
rm signingkeys.pfx
rm mgm.cpi
rm request1.csr
rm gradle-plugin-default-key.pem

rm -rf register-member