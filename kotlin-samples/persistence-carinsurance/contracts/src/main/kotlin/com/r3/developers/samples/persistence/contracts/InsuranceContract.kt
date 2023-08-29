package com.r3.developers.samples.persistence.contracts

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction


class InsuranceContract: Contract {

    class IssueInsurance: Command
    class AddClaim: Command

    override fun verify(transaction: UtxoLedgerTransaction) {
        // Add contract validation logic here.
    }

}