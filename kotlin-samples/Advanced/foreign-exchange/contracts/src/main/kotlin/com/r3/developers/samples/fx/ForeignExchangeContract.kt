package com.r3.developers.samples.fx

import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ForeignExchangeContract: Contract {

    internal companion object {
        const val REQUIRE_SINGLE_COMMAND = "A single ForeignExchangeCommand is required."
        const val UNRECOGNIZED_COMMAND = "An unrecognized Command is given:"
        const val REQUIRE_SINGLE_OUTPUT_STATE = "The transaction with the following command requires a single output state:"
        const val NON_POSITIVE = "The requested transaction amount is not greater than 0"
        const val NON_MATCHING = "The proposed output state variables does not match the inputs of the command."
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(ForeignExchangeCommands::class.java).singleOrNull()
            ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)
        val commandName = command::class.java.name
        val output = transaction.getOutputStates(ForeignExchange::class.java).singleOrNull()
            ?: throw CordaRuntimeException("$REQUIRE_SINGLE_OUTPUT_STATE $commandName")

        when(command) {
            is ForeignExchangeCommands.Create -> {
                NON_POSITIVE using { output.amount > 0 }
                NON_MATCHING using {
                    command.amount == output.amount &&
                    command.convertingFrom == output.convertingFrom &&
                    command.convertingTo == output.convertingTo &&
                    command.exchangeRate == output.exchangeRate &&
                    command.convertedAmount == output.convertedAmount &&
                    command.status == output.status
                }
            }
            else -> {
                throw IllegalArgumentException("$UNRECOGNIZED_COMMAND $commandName.")
            }
        }
    }

    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed contract requirement: $this")
    }

}