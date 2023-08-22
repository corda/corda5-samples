# Foreign Exchange

//todo change

This CorDapp allows nodes to:

* Request the Nth prime number
* Request the Prime Service Node's signature to prove that the number included in their transaction is actually the Nth prime number

## Concepts

This CorDapp is built as followed:
1. A state, contract and command(s) definition is created to define a state where the `nthPrimeNumber` is calculated given then index `n`.
2. A `PrimeService` class is written separately from workflows, with the intention that it can be used by workflows.
2. The main `CreatePrimeFlow` workflow is written such that it:
   1. starts the `QueryPrimeSubFlow` subflow to query the service to calculate the Nth prime
   2. creates a 'signedUtxoTransaction' proposing the prime number state
   3. starts the `FinalizePrimeSubFlow` subflow to query the service node to verify, sign and finalize the transaction
   4. returns the result as a readable string.

The `logs` directory highlights the control flow and outputs helpful information throughout the entire workflow lifecycle.

## Pre-Requisites

Refer to `Developing Applications > Getting Started Using the CSDE > Prerequisites for the CSDE` section within the latest corda documentation, found in https://docs.r3.com