package com.r3.developers.samples.negotiation.workflows.Accept

import net.corda.v5.base.annotations.CordaSerializable
import java.util.*

@CordaSerializable
data class AcceptFlowArgs(
    var proposalID: UUID
)
