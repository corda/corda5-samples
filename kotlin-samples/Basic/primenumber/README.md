# Prime Number

This CorDapp allows nodes to:

* Request the Nth prime number
* Request the Prime Service Node's signature to prove that the number included in their transaction is actually the Nth prime number

## Concepts

This repo is split into three CorDapps:

1. A base CorDapp, which includes the state and contract definition, as well as some utility flows that need to be
   shared by both the Oracle service and the client
2. A client CorDapp, which implements a flow to create numbers involving oracle-validated prime numbers
3. A service, which implements the primes oracle

## Pre-Requisites

Refer to `Developing Applications > Getting Started Using the CSDE > Prerequisites for the CSDE` section within the latest corda documentation, found in https://docs.r3.com