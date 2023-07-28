package com.r3.developers.samples.primenumber.flows

import com.r3.developers.samples.primenumber.services.Oracle
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import org.slf4j.LoggerFactory

@InitiatedBy(protocol = "query-prime")
class QueryPrimeResponderSubFlow(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val RECEIVING = "Receiving query request."
        const val CALCULATING = "Calculating Nth prime."
        const val SENDING = "Sending query response."
    }

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("oracle-primenumber: QueryPrimeResponderSubFlow.call() called")

        //1. Receiving Message
        log.info(RECEIVING)
        val receivedMessage = session.receive(QueryPrimeRequest::class.java)
        log.info("receivedMessage $receivedMessage")

        val oracleService = Oracle();

        //2. Calculating
        log.info(CALCULATING)
        val response: QueryPrimeResponse = try {
            //Get the nth prime from the oracle.
            val result = oracleService.query(receivedMessage.n)
            QueryPrimeResponse(result)
        } catch(e: Exception) {
            throw CordaRuntimeException("$e")
        }

        //3. Replying with new Message
        log.info(SENDING)
        session.send(response)
    }
}