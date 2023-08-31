package com.r3.developers.samples.negotiation.workflows.Propose

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name

@CordaSerializable
data class ProposalFlowArgs(
    var amount: Int,
    var counterParty: MemberX500Name,
    var isBuyer:Boolean

)
