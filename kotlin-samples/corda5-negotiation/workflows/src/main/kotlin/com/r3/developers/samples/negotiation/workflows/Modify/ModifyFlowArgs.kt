package com.r3.developers.samples.negotiation.workflows.Modify

import net.corda.v5.base.annotations.CordaSerializable
import java.util.*

@CordaSerializable
data class ModifyFlowArgs (
    var proposalID: UUID,
    var newAmount: Int
)
