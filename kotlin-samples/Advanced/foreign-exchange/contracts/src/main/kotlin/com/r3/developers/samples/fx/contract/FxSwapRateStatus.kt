package com.r3.developers.samples.fx.contract

import net.corda.v5.base.annotations.CordaSerializable

@CordaSerializable
enum class FxSwapRateStatus { PROPOSED, ACCEPTED, REJECTED }