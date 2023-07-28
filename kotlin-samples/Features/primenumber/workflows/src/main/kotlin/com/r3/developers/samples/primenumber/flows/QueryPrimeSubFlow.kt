package com.r3.developers.samples.primenumber.flows

import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "query-prime")
class QueryPrimeSubFlow(private val oracle: MemberX500Name, private val n: Int): SubFlow<Int> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): Int {
        //1. logging
        log.info("oracle-primenumber: QueryPrimeSubFlow.call() called")

        //2. Input Processing
        log.info("oracle: $oracle | n: $n")

        //3. Establishing Connection with oracle node
        val session = flowMessaging.initiateFlow(oracle)

        val message = QueryPrimeRequest(n)

        val receivedMessage = session.sendAndReceive(QueryPrimeResponse::class.java,message)

        return receivedMessage.n
    }
}