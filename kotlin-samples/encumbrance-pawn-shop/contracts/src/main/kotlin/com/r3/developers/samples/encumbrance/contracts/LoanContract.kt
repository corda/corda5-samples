package com.r3.developers.samples.encumbrance.contracts

import com.r3.developers.samples.encumbrance.states.Asset
import com.r3.developers.samples.encumbrance.states.Loan
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction


class LoanContract: Contract {

    internal companion object {
        const val REQUIRES_ONE_COMMAND = "The transaction requires one Loan command"
        const val REQUIRES_ONE_INPUT = "The transaction requires one input"
        const val REQUIRES_TWO_INPUTS = "The transaction requires two inputs"
        const val REQUIRES_ONE_ASSET_INPUT = "The transaction requires one Asset input"
        const val REQUIRES_ONE_LOAN_INPUT = "The transaction requires one Loan input"
        const val REQUIRES_TWO_OUTPUTS = "The transaction requires two outputs"
        const val REQUIRES_ONE_OUTPUT = "The transaction requires one output"
        const val REQUIRES_ONE_ASSET_OUTPUT = "The transaction requires one Asset output"
        const val REQUIRES_ONE_LOAN_OUTPUT = "The transaction requires one Loan output"
        const val REQUIRES_LENDER_SIGN = "The transaction must be signed by the lender"
    }

    interface LoanCommands : Command {
        class Issue : LoanCommands {}
        class Settle : LoanCommands {}
    }


    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(LoanCommands::class.java).singleOrNull() ?: throw CordaRuntimeException(
            REQUIRES_ONE_COMMAND
        )
        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is LoanCommands.Issue -> {
                REQUIRES_ONE_INPUT  using(transaction.inputContractStates.size == 1 )
                REQUIRES_ONE_ASSET_INPUT using(transaction.getInputStates(Asset::class.java).size == 1 )

                REQUIRES_TWO_OUTPUTS using(transaction.outputContractStates.size == 2)
                REQUIRES_ONE_ASSET_OUTPUT using(transaction.getOutputStates(Asset::class.java).size == 1)
                REQUIRES_ONE_LOAN_OUTPUT using(transaction.getOutputStates(Loan::class.java).size == 1)

                REQUIRES_LENDER_SIGN using (transaction.signatories.contains(transaction.getOutputStates(Loan::class.java)[0].lender.ledgerKey))
            }
            // Rules applied only to transactions with the Update Command.
            is LoanCommands.Settle -> {
                REQUIRES_TWO_INPUTS using (transaction.inputContractStates.size == 2 )
                REQUIRES_ONE_ASSET_INPUT using (transaction.getInputStates(Asset::class.java).size == 1 )
                REQUIRES_ONE_LOAN_INPUT using (transaction.getInputStates(Loan::class.java).size == 1 )

                REQUIRES_ONE_OUTPUT using (transaction.outputContractStates.size == 1 )
                REQUIRES_ONE_ASSET_OUTPUT using (transaction.getOutputStates(Asset::class.java).size == 1 )
            }
            else -> {
                throw CordaRuntimeException("Invalid Command")
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