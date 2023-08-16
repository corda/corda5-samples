package com.r3.developers.samples.referencestate.contracts

import com.r3.developers.samples.referencestate.states.SanctionList
import com.r3.developers.samples.referencestate.states.SanctionableIOUState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class SanctionableIOUContract :Contract {

    internal companion object {
        val REQUIRES_CREATE_COMMAND = "The transaction requires the create command"
        val REQUIRES_SANCTIONED_ENTITY = "All transactions require a list of sanctioned entities"
        val REQUIRES_ZERO_INPUT = "No inputs should be consumed when issuing an IOU."
        val REQUIRES_ONE_OUTPUT = "Only one output states should be produced."
        val REQUIRES_ONE_IOU_OUTPUT = "Only one output states of type SanctionableIOUState should be produced."
        val REQUIRE_DIFFERENT_LENDER_BORROWER = "The lender and the borrower cannot be the same entity."
        val REQUIRE_ALL_SIGNATURE = "All of the participants must be signers."
        val REQUIRE_POSITIVE_IOU_VALUE = "The IOU's value must be non-negative."
    }

    interface SanctionableIOUCommands : Command {
        class Create(val sanctionAuthority : MemberX500Name): SanctionableIOUCommands
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(SanctionableIOUCommands::class.java).singleOrNull()  ?: throw CordaRuntimeException(
            REQUIRES_CREATE_COMMAND
        )
        val sanctionList = transaction.getReferenceStates(SanctionList::class.java).singleOrNull() ?: throw CordaRuntimeException(
            REQUIRES_SANCTIONED_ENTITY
        )

        when(command){
            is SanctionableIOUCommands.Create ->{
                "The "+sanctionList.issuer.name.organization+" is the wrong sanction list issuing authority for this IOU state. " +
                        "This IOU state will follow the sanction list issued by "+ command.sanctionAuthority using (sanctionList.issuer.name == command.sanctionAuthority)
                REQUIRES_ZERO_INPUT using (transaction.inputContractStates.isEmpty())
                REQUIRES_ONE_OUTPUT using (transaction.outputContractStates.size == 1)
                REQUIRES_ONE_IOU_OUTPUT using (transaction.getOutputStates(SanctionableIOUState::class.java).size == 1)

                val output = transaction.getOutputStates(SanctionableIOUState::class.java)[0]
                REQUIRE_DIFFERENT_LENDER_BORROWER using (output.borrower!=output.lender)
                REQUIRE_ALL_SIGNATURE using (transaction.signatories.containsAll(output.participants))
                REQUIRE_POSITIVE_IOU_VALUE using (output.value > 0)

                val badPeopleKeys = sanctionList.badPeople.map { it -> it.ledgerKey }.toSet()
                "The lender + " + output.lender.name + " is a sanctioned entity" using (!badPeopleKeys.contains(output.lender.ledgerKey))
                "The borrower + " + output.borrower.name + " is a sanctioned entity" using (!badPeopleKeys.contains(output.borrower.ledgerKey))
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