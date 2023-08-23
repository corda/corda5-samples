package com.r3.developers.samples.negotiation.workflows.util

import java.util.*

class ListProposalArgs {
    var proposalID: UUID? = null
        private set
    var amount = 0
        private set
    var buyer: String? = null
        private set
    var seller: String? = null
        private set
    var proposer: String? = null
        private set
    var proposee: String? = null
        private set

    constructor()
    constructor(proposalID: UUID?, amount: Int, buyer: String?, seller: String?, proposer: String?, proposee: String?) {
        this.proposalID = proposalID
        this.amount = amount
        this.buyer = buyer
        this.seller = seller
        this.proposer = proposer
        this.proposee = proposee
    }
}
