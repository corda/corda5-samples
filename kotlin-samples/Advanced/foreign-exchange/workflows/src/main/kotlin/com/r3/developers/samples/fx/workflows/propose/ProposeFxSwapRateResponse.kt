package com.r3.developers.samples.fx.workflows.propose

import com.r3.developers.samples.fx.contract.CurrencyCode
import com.r3.developers.samples.fx.contract.FxSwapRateStatus
import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal
import java.time.Instant

internal data class ProposeFxSwapRateResponse(
    val exchangeRateService: MemberX500Name,
    val initiator: MemberX500Name,
    val responder: MemberX500Name,
    val convertingFrom: CurrencyCode,
    val convertingTo: CurrencyCode,
    val amount: BigDecimal,
    val convertedAmount: BigDecimal,
    val expires: Instant,
    val status: FxSwapRateStatus
)
