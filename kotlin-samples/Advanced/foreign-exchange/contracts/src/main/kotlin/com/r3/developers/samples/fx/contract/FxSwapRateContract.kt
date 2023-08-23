package com.r3.developers.samples.fx.contract

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.math.BigDecimal

class FxSwapRateContract : Contract {

    internal companion object {
        const val CONTRACT_RULE_COMMANDS =
            "On transaction executing, exactly one FxSwapRateCommand command must be present in the transaction."
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.getCommands(FxSwapRateCommand::class.java).singleOrNull()
        val command = checkNotNull(commands) { CONTRACT_RULE_COMMANDS }

        command.verify(transaction)
    }

    private interface FxSwapRateCommand : Command {
        fun verify(transaction: UtxoLedgerTransaction)
    }

    class Propose : FxSwapRateCommand {
        internal companion object {
            const val CONTRACT_RULE_INPUTS =
                "On fx swap rate proposing, zero fx swap rate states must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On fx swap rate proposing, only one fx swap rate state must be created."

            const val CONTRACT_RULE_PARTICIPANTS =
                "On fx swap rate proposing, the initiator and responder must not be equal."

            const val CONTRACT_RULE_CONVERSION =
                "On fx swap rate proposing, the converting from and converting to currencies must not be equal."

            const val CONTRACT_RULE_AMOUNT =
                "On fx swap rate proposing, the proposed amount must be greater than zero."

            const val CONTRACT_RULE_EXCHANGE_RATE =
                "On fx swap rate proposing, the exchange rate must be greater than or equal to zero."

            const val CONTRACT_RULE_EXPIRES =
                "On fx swap rate proposing, the expiry date must exceed the transaction time window start."

            const val CONTRACT_RULE_STATUS =
                "On fx swap rate proposing, the status must be PROPOSED."

            const val CONTRACT_RULE_SIGNATORIES =
                "On fx swap rate proposing, the initiator must sign the transaction."
        }

        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(FxSwapRate::class.java)
            val outputs = transaction.getOutputStates(FxSwapRate::class.java)

            check(inputs.isEmpty()) { CONTRACT_RULE_INPUTS }
            check(outputs.size == 1) { CONTRACT_RULE_OUTPUTS }

            val output = outputs.single()

            check(output.initiator != output.responder) { CONTRACT_RULE_PARTICIPANTS }
            check(output.convertingFrom != output.convertingTo) { CONTRACT_RULE_CONVERSION }
            check(output.amount > BigDecimal.ZERO) { CONTRACT_RULE_AMOUNT }
            check(output.exchangeRate >= BigDecimal.ZERO) { CONTRACT_RULE_EXCHANGE_RATE }
            check(output.expires > transaction.timeWindow.from) { CONTRACT_RULE_EXPIRES }
            check(output.status == FxSwapRateStatus.PROPOSED) { CONTRACT_RULE_STATUS }
            check(output.initiator in transaction.signatories) { CONTRACT_RULE_SIGNATORIES }
        }
    }

    class Approve : FxSwapRateCommand {
        internal companion object {
            const val CONTRACT_RULE_INPUTS =
                "On fx swap rate approving, only one fx swap rate state must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On fx swap rate approving, only one fx swap rate state must be created."

            const val CONTRACT_RULE_CHANGES =
                "On fx swap rate approving, only the status must change."

            const val CONTRACT_RULE_INPUT_STATUS =
                "On fx swap rate approving, the status of the consumed state must be PROPOSED."

            const val CONTRACT_RULE_OUTPUT_STATUS =
                "On fx swap rate approving, the status of the created state must be ACCEPTED."

            const val CONTRACT_RULE_SIGNATORIES =
                "On fx swap rate approving, the responder must sign the transaction."
        }

        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(FxSwapRate::class.java)
            val outputs = transaction.getOutputStates(FxSwapRate::class.java)

            check(inputs.size == 1) { CONTRACT_RULE_INPUTS }
            check(outputs.size == 1) { CONTRACT_RULE_OUTPUTS }

            val input = inputs.single()
            val output = outputs.single()

            check(input.immutableEquals(output)) { CONTRACT_RULE_CHANGES }
            check(input.status == FxSwapRateStatus.PROPOSED) { CONTRACT_RULE_INPUT_STATUS }
            check(output.status == FxSwapRateStatus.ACCEPTED) { CONTRACT_RULE_OUTPUT_STATUS }
            check(output.responder in transaction.signatories) { CONTRACT_RULE_SIGNATORIES }
        }
    }

    class Reject : FxSwapRateCommand {
        internal companion object {
            const val CONTRACT_RULE_INPUTS =
                "On fx swap rate rejecting, only one fx swap rate state must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On fx swap rate rejecting, only one fx swap rate state must be created."

            const val CONTRACT_RULE_CHANGES =
                "On fx swap rate rejecting, only the status must change."

            const val CONTRACT_RULE_INPUT_STATUS =
                "On fx swap rate rejecting, the status of the consumed state must be PROPOSED."

            const val CONTRACT_RULE_OUTPUT_STATUS =
                "On fx swap rate rejecting, the status of the created state must be REJECTED."

            const val CONTRACT_RULE_SIGNATORIES =
                "On fx swap rate rejecting, the responder must sign the transaction."
        }

        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(FxSwapRate::class.java)
            val outputs = transaction.getOutputStates(FxSwapRate::class.java)

            check(inputs.size == 1) { CONTRACT_RULE_INPUTS }
            check(outputs.size == 1) { CONTRACT_RULE_OUTPUTS }

            val input = inputs.single()
            val output = outputs.single()

            check(input.immutableEquals(output)) { CONTRACT_RULE_CHANGES }
            check(input.status == FxSwapRateStatus.PROPOSED) { CONTRACT_RULE_INPUT_STATUS }
            check(output.status == FxSwapRateStatus.REJECTED) { CONTRACT_RULE_OUTPUT_STATUS }
            check(output.responder in transaction.signatories) { CONTRACT_RULE_SIGNATORIES }
        }
    }

    class Cancel : FxSwapRateCommand {
        internal companion object {
            const val CONTRACT_RULE_INPUTS =
                "On fx swap rate cancelling, only one fx swap rate state must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On fx swap rate cancelling, zero fx swap rate states must be created."

            const val CONTRACT_RULE_SIGNATORIES =
                "On fx swap rate cancelling, the initiator must sign the transaction."
        }

        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(FxSwapRate::class.java)
            val outputs = transaction.getOutputStates(FxSwapRate::class.java)

            check(inputs.size == 1) { CONTRACT_RULE_INPUTS }
            check(outputs.isEmpty()) { CONTRACT_RULE_OUTPUTS }

            val input = inputs.single()

            check(input.initiator in transaction.signatories) { CONTRACT_RULE_SIGNATORIES }
        }
    }
}