package com.r3.developers.pingpong.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@InitiatedBy(protocol = "ping")
class Pong : ResponderFlow {
    // FlowMessaging provides a service that establishes flow sessions between virtual nodes
    // that send and receive payloads between them.
    @CordaInject
    var flowMessaging: FlowMessaging? = null

    // MemberLookup provides a service for looking up information about members of the virtual network which
    // this CorDapp operates in.
    @CordaInject
    var memberLookup: MemberLookup? = null

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("Pong.call() called")
        val message = session.receive<String>(String::class.java)
        if (message == "ping") {
            log.info("Received Ping")
            session.send("pong")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Ping::class.java)
    }
}