package com.r3.developers.samples.primenumber.workflows

import com.r3.developers.samples.primenumber.services.PrimeService
import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import org.slf4j.LoggerFactory

// This class initiates the subflow session with the primeService vNode requesting for the Nth prime given index n
// then receives the message from the corresponding subflow and returns the
@InitiatingFlow(protocol = "query-prime")
class QueryPrimeSubFlow(private val primeServiceName: MemberX500Name, private val n: Int, private val primeService: PrimeService): SubFlow<QueryPrimeResponse> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "QueryPrimeSubFlow.call() called"
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): QueryPrimeResponse {
        log.info(FLOW_CALL)

        val session = flowMessaging.initiateFlow(primeServiceName)
        val message = QueryPrimeRequest(n, primeService)

        return session.sendAndReceive(QueryPrimeResponse::class.java,message)
    }
}

// This class handles the initiating flow to request for the Nth prime, calls the primeService's queryNthPrime method
// and returns the response back to the initiating QueryPrimeSubFlow subflow
@InitiatedBy(protocol = "query-prime")
class QueryPrimeResponderSubFlow(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "QueryPrimeResponderSubFlow.call() called"
        const val RECEIVING = "Receiving query request."
        const val CALCULATING = "Calculating Nth prime."
        const val SENDING = "Sending query response."
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)

        log.info(RECEIVING)
        val receivedMessage = session.receive(QueryPrimeRequest::class.java)
        log.info("receivedMessage $receivedMessage")
        val primeService = receivedMessage.primeService
        val n = receivedMessage.n

        log.info(CALCULATING)
        val response: QueryPrimeResponse = try {
            primeService.queryNthPrime(n,primeService)
        } catch(e: Exception) {
            throw CordaRuntimeException("$e")
        }

        log.info(SENDING)
        session.send(response)
    }
}