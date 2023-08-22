package com.r3.developers.samples.fx.primetodelete

import net.corda.v5.ledger.utxo.Command

// This is used by the CreatePrimeFlow workflow class and is used to add a command to a proposed UtxoTransaction
interface PrimeCommands : Command {

    class Query(val index: Int): PrimeCommands

    //The command requires the arguments 'n' and 'nthPrime', which is used for contract verification
    class Create(val n: Int, val nthPrime: Int): PrimeCommands
}