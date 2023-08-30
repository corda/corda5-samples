package com.r3.developers.samples.tokens.contracts

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction


class GoldContract: Contract {

    class Issue: Command
    class Transfer: Command
    class Burn: Command

    override fun verify(transaction: UtxoLedgerTransaction) {
        // Add contract validation logic here.
    }

}