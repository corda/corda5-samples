package com.r3.developers.samples.primenumber

import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.lang.IllegalArgumentException

class PrimeContract: Contract {

    internal companion object {
        const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
        const val UNRECOGNISED_COMMAND = "Incorrect type of Prime command"
        const val NON_MATCHING = "The prime in the output does not match the prime in the command."
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)
        val output = transaction.getOutputStates(Prime::class.java).first()

        when(command) {
            is PrimeCommands.Create -> {
                NON_MATCHING using {
                    (command.n == output.n && command.nthPrime == output.nthPrime)
                }
            }
            else -> {
                throw IllegalArgumentException("${UNRECOGNISED_COMMAND}: ${command::class.java.name}")
            }
        }
    }

    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }

}