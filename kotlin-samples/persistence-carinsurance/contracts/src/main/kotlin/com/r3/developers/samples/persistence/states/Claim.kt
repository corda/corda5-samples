package com.r3.developers.samples.persistence.states

import net.corda.v5.base.annotations.CordaSerializable

/**
 * Simple POJO class for the claim details.
 */
@CordaSerializable
data class Claim(val claimNumber: String,
                 val claimDescription: String,
                 val claimAmount: Int)