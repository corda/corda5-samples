package com.r3.developers.samples.negotiation

import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ProposalAndTradeContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        // Extract the command from the transaction
        // Verify the transaction according to the intention of the transaction
        val command = transaction.commands[0] as NegotiationCommands
        command.verify(transaction)
    }

    interface NegotiationCommands : Command {
        fun verify(transaction: UtxoLedgerTransaction?)
    }

    class Propose : NegotiationCommands {
        var noInputsMsg = "There are no inputs"
        var oneOutputMsg = "Only one output state should be created."
        var outputTypeMsg = "The single output is of type ProposalState"
        var commandMsg = "There is exactly one command"
        var buyerMsg = "The buyer are the proposer"
        var sellerMsg = "The seller are the proposee"
        var proposerMsg = "The proposer is a required signer"
        var proposeeMsg = "The proposee is a required signer"

        override fun verify(transaction: UtxoLedgerTransaction?) {
            val proposalStateOutput = transaction!!.getOutputStates(Proposal::class.java)[0]

            require(transaction.inputStateAndRefs.isEmpty(), noInputsMsg)
            require(transaction.outputTransactionStates.size == 1, oneOutputMsg)
            require(transaction.getOutputStates(Proposal::class.java).size == 1, outputTypeMsg)
            require(transaction.commands.size == 1, commandMsg)
            require(proposalStateOutput.proposer.toString() == proposalStateOutput.buyer.toString(), buyerMsg)
            require(proposalStateOutput.proposee.toString() == proposalStateOutput.seller.toString(), sellerMsg)
            require(transaction.signatories.contains(proposalStateOutput.proposer.ledgerKey), proposerMsg)
            require(transaction.signatories.contains(proposalStateOutput.proposee.ledgerKey), proposeeMsg)
        }
    }

    class Modify : NegotiationCommands {
        var oneInputMsg = "There is exactly one input"
        var inputTypeMsg = "The single input is of type ProposalState"
        var oneOutputMsg = "There is exactly one output"
        var outputTypeMsg = "The single output is of type ProposalState"
        var oneCommandMsg = "There is exactly one command"
        var amountModifiedMsg = "The amount is modified in the output"
        var buyerMsg = "The buyer is unmodified in the output"
        var sellerMsg = "The seller is unmodified in the output"
        var proposerMsg = "The proposer is a required signer"
        var proposeeMsg = "The proposee is a required signer"
        override fun verify(transaction: UtxoLedgerTransaction?) {
            val proposalStateOutput = transaction!!.getOutputStates(Proposal::class.java)[0]
            val proposalStateInputs = transaction.getInputStates(Proposal::class.java)[0]

            require(transaction.inputStateAndRefs.size == 1, oneInputMsg)
            require(transaction.getInputStates(Proposal::class.java).size == 1, inputTypeMsg)
            require(transaction.outputTransactionStates.size == 1, oneOutputMsg)
            require(transaction.getOutputStates(Proposal::class.java).size == 1, outputTypeMsg)
            require(transaction.commands.size == 1, oneCommandMsg)
            require(proposalStateOutput.amount != proposalStateInputs.amount, amountModifiedMsg)
            require(proposalStateInputs.buyer.toString() == proposalStateOutput.buyer.toString(), buyerMsg)
            require(proposalStateInputs.seller.toString() == proposalStateOutput.seller.toString(), sellerMsg)

            require(transaction.signatories.contains(proposalStateInputs.proposer.ledgerKey), proposerMsg)
            require(transaction.signatories.contains(proposalStateInputs.proposee.ledgerKey), proposeeMsg)
        }
    }

    class Accept : NegotiationCommands {

        var oneInputMsg = "There is exactly one input"
        var inputTypeMsg = "The single input is of type ProposalState"
        var oneOutputMsg = "There is exactly one output"
        var outputTypeMsg = "The single output is of type TradeState"
        var oneCommandMsg = "There is exactly one command"
        var amountMsg = "The amount is unmodified in the output"
        var buyerMsg = "The buyer is unmodified in the output"
        var sellerMsg = "The seller is unmodified in the output"
        var proposerMsg = "The proposer is a required signer"
        var proposeMsg = "The propose is a required signer"
        var proposerCannotAcceptProposalMsg = "Modifier cannot accept the proposal"
        override fun verify(transaction: UtxoLedgerTransaction?) {
            val tradeStateOutput = transaction!!.getOutputStates(Trade::class.java)[0]
            val proposalStateInputs = transaction.getInputStates(Proposal::class.java)[0]

            require(transaction.inputStateAndRefs.size == 1, oneInputMsg)
            require(transaction.getInputStates(Proposal::class.java).size == 1, inputTypeMsg)
            require(transaction.outputTransactionStates.size == 1, oneOutputMsg)
            require(transaction.getOutputStates(Trade::class.java).size == 1, outputTypeMsg)
            require(transaction.commands.size == 1, oneCommandMsg)
            require(tradeStateOutput.amount == proposalStateInputs.amount, amountMsg)
            require(proposalStateInputs.buyer.toString() == tradeStateOutput.buyer.toString(), buyerMsg)
            require(
                !proposalStateInputs.modifier?.name.toString().equals(tradeStateOutput.acceptor.toString()),
                proposerCannotAcceptProposalMsg
            )
            require(proposalStateInputs.seller.toString() == tradeStateOutput.seller.toString(), sellerMsg)
            require(transaction.signatories.contains(proposalStateInputs.proposer.ledgerKey), proposerMsg)
            require(
                transaction.signatories.contains(proposalStateInputs.proposee.ledgerKey), proposeMsg
            )
        }
    }


    companion object {
        private fun require(asserted: Boolean, errorMessage: String) {
            if (!asserted) {
                throw CordaRuntimeException(errorMessage)
            }
        }
    }
}
