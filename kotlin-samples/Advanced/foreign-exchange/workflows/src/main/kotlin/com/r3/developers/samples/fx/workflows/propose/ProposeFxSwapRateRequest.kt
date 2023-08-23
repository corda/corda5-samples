package com.r3.developers.samples.fx.workflows.propose

import com.r3.developers.samples.fx.contract.CurrencyCode
import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal

internal data class ProposeFxSwapRateRequest(
    val exchangeRateService: MemberX500Name,
    val responder: MemberX500Name,
    val convertingFrom: CurrencyCode,
    val convertingTo: CurrencyCode,
    val amount: BigDecimal,
    val notary: MemberX500Name
)
