package com.r3.developers.samples.fx

import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.math.BigDecimal

class ForeignExchangeContract: Contract {

    internal companion object {
        const val CONTRACT_RULE_SINGLE_COMMAND = "Exactly one ForeignExchangeCommands command must be present in the transaction."
        const val CONTRACT_RULE_UNRECOGNIZED_COMMAND = "An unrecognized Command is given:"
        const val CONTRACT_RULE_SINGLE_OUTPUT = "A ForeignExchange transaction requires a single output state to be created."
        const val NON_POSITIVE = "The requested transaction amount is not greater than 0."
        const val NON_MATCHING = "The proposed output state variables does not match the inputs of the command."
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(ForeignExchangeCommands::class.java).singleOrNull()
            ?: throw CordaRuntimeException(CONTRACT_RULE_SINGLE_COMMAND)
        val commandName = command::class.java.name
        val output = transaction.getOutputStates(ForeignExchange::class.java).singleOrNull()
            ?: throw CordaRuntimeException("$CONTRACT_RULE_SINGLE_OUTPUT")

        when(command) {
            is ForeignExchangeCommands.Create -> {
                NON_POSITIVE using { output.amount > BigDecimal(0) }
                NON_MATCHING using {
                    command.initiatorIdentity == output.initiatorIdentity &&
                    command.recipientIdentity == output.recipientIdentity &&
                    command.amount == output.amount &&
                    command.convertingFrom == output.convertingFrom &&
                    command.convertingTo == output.convertingTo &&
                    command.exchangeRate == output.exchangeRate &&
                    command.convertedAmount == output.convertedAmount &&
                    command.status == output.status
                }
            }
            else -> {
                throw IllegalArgumentException("$CONTRACT_RULE_UNRECOGNIZED_COMMAND $commandName.")
            }
        }
    }

    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed contract requirement: $this")
    }

}