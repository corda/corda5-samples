package com.r3.developers.samples.primenumber

import net.corda.v5.ledger.utxo.Command

interface PrimeCommands : Command {
    class Create(val n: Int, val nthPrime: Int): PrimeCommands
}