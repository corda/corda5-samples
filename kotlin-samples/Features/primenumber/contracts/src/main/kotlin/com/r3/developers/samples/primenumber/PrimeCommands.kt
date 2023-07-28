package com.r3.developers.samples.primenumber

import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.Command

interface PrimeCommands : Command {

    // Commands signed by oracles must contain the facts the oracle is attesting to.
    class Create(val n: Int, val nthPrime: Int): PrimeCommands
}