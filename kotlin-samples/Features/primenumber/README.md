# Prime Number -- Oracle

This CorDapp allows nodes to:

* Request the Nth prime number
* Request the oracle's signature to prove that the number included in their transaction is actually the Nth prime
  number

Whilst the functionality is superfluous (as primes can be verified deterministically via the contract code), this
CorDapp is a simple example of how to structure an oracle service that provides querying and signing abilities. In the
real world, oracles would instead provide and sign statements about stock prices, exchange rates, and other data.


## Concepts

This repo is split into three CorDapps:

1. A base CorDapp, which includes the state and contract definition, as well as some utility flows that need to be
   shared by both the Oracle service and the client
2. A client CorDapp, which implements a flow to create numbers involving oracle-validated prime numbers
3. A service, which implements the primes oracle


## Usage


## Pre-Requisites


### Running the CorDapp

