package com.r3.developers.samples.negotiation.workflows.Propose

import net.corda.v5.base.annotations.CordaSerializable

@CordaSerializable
data class ProposalFlowArgs(
    var amount: Int,
    var counterParty: String,
    var isBuyer:Boolean

)
