package com.r3.developers.samples.referencestate.contracts

import com.r3.developers.samples.referencestate.states.SanctionList
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class SanctionListContract :Contract {

    internal companion object {
        const val REQUIRES_ONE_COMMAND = "The transaction requires exactly one command"
        const val CREATE_REQUIRES_ZERO_INPUTS = "When creating a sanctions list there should be no inputs"
        const val CREATE_REQUIRES_ONE_OUTPUT = "When creating a sanctions list there should be exactly one output"
        const val CREATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT = "When creating a sanctions list there should be one output of type SanctionedEntities"
        const val CREATE_REQUIRES_ISSUER_SIGNATURE = "The issuer of the sanctions list must sign"

        const val UPDATE_REQUIRES_ONE_INPUT = "There must be exactly one input when updating Sanctions List"
        const val UPDATE_REQUIRES_ONE_OUTPUT = "There must be exactly one output when updating Sanctions List"
        const val UPDATE_REQUIRES_ONE_SANCTION_ENTITY_INPUT = "There must be exactly one input Sanctions List when updating"
        const val UPDATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT = "There must be exactly one input Sanctions List when updating"
        const val UPDATE_ISSUER_SHOULD_NOT_CHANGE = "The issuer must remain the same across an update"
    }

    interface SanctionListCommand : Command {
        class Create: SanctionListCommand
        class Update: SanctionListCommand
    }

    override fun verify(transaction: UtxoLedgerTransaction) {

        val command = transaction.getCommands(SanctionListCommand::class.java).singleOrNull()  ?: throw CordaRuntimeException(REQUIRES_ONE_COMMAND)
        when(command){
            is SanctionListCommand.Create -> {
                CREATE_REQUIRES_ZERO_INPUTS using (transaction.inputContractStates.isEmpty())
                CREATE_REQUIRES_ONE_OUTPUT using (transaction.outputContractStates.size == 1)
                CREATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT using (transaction.getOutputStates(SanctionList::class.java).size == 1)
                val output = transaction.getOutputStates(SanctionList::class.java)[0]
                CREATE_REQUIRES_ISSUER_SIGNATURE using (transaction.signatories.contains(output.issuer.ledgerKey))
            }
            is SanctionListCommand.Update -> {
                UPDATE_REQUIRES_ONE_INPUT using (transaction.inputContractStates.size == 1)
                UPDATE_REQUIRES_ONE_OUTPUT using (transaction.outputContractStates.size == 1)
                UPDATE_REQUIRES_ONE_SANCTION_ENTITY_INPUT using (transaction.getInputStates(SanctionList::class.java).size == 1)
                UPDATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT using (transaction.getOutputStates(SanctionList::class.java).size == 1)
                val input = transaction.getInputStates(SanctionList::class.java)[0]
                val output = transaction.getOutputStates(SanctionList::class.java)[0]
                UPDATE_ISSUER_SHOULD_NOT_CHANGE using (input.issuer.name==output.issuer.name)
            }else -> {
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