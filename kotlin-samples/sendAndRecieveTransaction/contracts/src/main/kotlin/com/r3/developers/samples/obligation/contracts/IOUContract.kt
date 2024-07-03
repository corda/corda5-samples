package com.r3.developers.samples.obligation.contracts

import com.r3.developers.samples.obligation.states.IOUState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class IOUContract : Contract {

    //IOU Commands
    class Issue: Command
    class Settle: Command
    class Transfer: Command

    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException("Requires a single command.")

        "The output state should have two and only two participants." using {
            val output = transaction.outputContractStates.first() as IOUState
            output.participants.size== 2
        }
        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Issue Command.
            is Issue -> {
                "When command is Create there should be one and only one output state." using (transaction.outputContractStates.size == 1)
            }
            // Rules applied only to transactions with the Settle Command.
            is Settle -> {
                "When command is Update there should be one and only one output state." using (transaction.outputContractStates.size == 1)
            }
            // Rules applied only to transactions with the Transfer Command.
            is Transfer -> {
                "When command is Update there should be one and only one output state." using (transaction.outputContractStates.size == 1)
            }
            else -> {
                throw CordaRuntimeException("Command not allowed.")
            }
        }
    }

    // Helper function to allow writing constraints in the Corda 4 '"text" using (boolean)' style
    private infix fun String.using(expr: Boolean) {
        if (!expr) throw CordaRuntimeException("Failed requirement: $this")
    }

    // Helper function to allow writing constraints in '"text" using {lambda}' style where the last expression
    // in the lambda is a boolean.
    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }
}