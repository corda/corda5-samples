package com.r3.developers.samples.fx.workflows.propose

import com.r3.developers.samples.fx.contract.FxSwapRate
import com.r3.developers.samples.fx.contract.FxSwapRateStatus
import com.r3.developers.samples.fx.receive
import com.r3.developers.samples.fx.workflows.oracle.GetExchangeRateFlow
import com.r3.developers.samples.fx.workflows.oracle.GetExchangeRateFlowResponder
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import java.time.Duration
import java.time.Instant

internal object ProposeFxSwapRateApplicationFlows {

    private const val FLOW_PROTOCOL = "propose-fx-swap-rate"
    private const val FLOW_PROTOCOL_VERSION = 1

    @CordaSerializable
    private enum class FlowType { EXCHANGE_RATE_SERVICE, EXCHANGE_RATE_TRANSACTION }

    @InitiatingFlow(protocol = FLOW_PROTOCOL, version = [FLOW_PROTOCOL_VERSION])
    class Initiator : ClientStartableFlow {

        @CordaInject
        private lateinit var jsonMarshallingService: JsonMarshallingService

        @CordaInject
        private lateinit var memberLookup: MemberLookup

        @CordaInject
        private lateinit var notaryLookup: NotaryLookup

        @CordaInject
        private lateinit var flowMessaging: FlowMessaging

        @CordaInject
        private lateinit var flowEngine: FlowEngine

        @Suspendable
        override fun call(requestBody: ClientRequestBody): String {
            val request = requestBody.getRequestBodyAs(jsonMarshallingService, ProposeFxSwapRateRequest::class.java)

            val fxSwapRate = request.getFxSwapRate()
            val transaction = request.getFxSwapRateTransaction(fxSwapRate)

            val response = ProposeFxSwapRateResponse(
                exchangeRateService = request.exchangeRateService,
                initiator = memberLookup.myInfo().name,
                responder = request.responder,
                convertingFrom = request.convertingFrom,
                convertingTo = request.convertingTo,
                amount = fxSwapRate.amount,
                convertedAmount = fxSwapRate.convertedAmount,
                expires = fxSwapRate.expires,
                status = fxSwapRate.status
            )

            return jsonMarshallingService.format(response)
        }

        @Suspendable
        private fun ProposeFxSwapRateRequest.getFxSwapRate(): FxSwapRate {
            val session = flowMessaging.initiateFlow(exchangeRateService)

            session.send(FlowType.EXCHANGE_RATE_SERVICE)

            val flow = GetExchangeRateFlow(session, convertingFrom, convertingTo)

            return FxSwapRate(
                memberLookup.myInfo().ledgerKeys.first(),
                memberLookup.lookup(responder)!!.ledgerKeys.first(),
                convertingFrom = convertingFrom,
                convertingTo = convertingTo,
                amount = amount,
                exchangeRate = flowEngine.subFlow(flow),
                expires = Instant.now().plus(Duration.ofDays(1)),
                status = FxSwapRateStatus.PROPOSED
            )
        }

        @Suspendable
        private fun ProposeFxSwapRateRequest.getFxSwapRateTransaction(fxSwapRate: FxSwapRate): UtxoSignedTransaction {
            val session = flowMessaging.initiateFlow(responder)
            val notaryInfo = notaryLookup.notaryServices.first()

            session.send(FlowType.EXCHANGE_RATE_TRANSACTION)

            val flow = ProposeFxSwapRateFlow(fxSwapRate, notaryInfo, listOf(session))
            return flowEngine.subFlow(flow)
        }
    }

    @InitiatedBy(protocol = FLOW_PROTOCOL, version = [FLOW_PROTOCOL_VERSION])
    class Responder : ResponderFlow {

        @CordaInject
        private lateinit var flowEngine: FlowEngine

        @Suspendable
        override fun call(session: FlowSession) {
            when (session.receive<FlowType>()) {
                FlowType.EXCHANGE_RATE_SERVICE -> flowEngine.subFlow(GetExchangeRateFlowResponder(session))
                FlowType.EXCHANGE_RATE_TRANSACTION -> flowEngine.subFlow(ProposeFxSwapRateFlowResponder(session))
            }
        }
    }
}
