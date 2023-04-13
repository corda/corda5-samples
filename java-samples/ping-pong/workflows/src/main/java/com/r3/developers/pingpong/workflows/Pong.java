package com.r3.developers.pingpong.workflows;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.application.messaging.FlowMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InitiatedBy(protocol = "ping")
public class Pong implements ResponderFlow {
    private final static Logger log = LoggerFactory.getLogger(Ping.class);

    // FlowMessaging provides a service that establishes flow sessions between virtual nodes
    // that send and receive payloads between them.
    @CordaInject
    public FlowMessaging flowMessaging;

    // MemberLookup provides a service for looking up information about members of the virtual network which
    // this CorDapp operates in.
    @CordaInject
    public MemberLookup memberLookup;

    public Pong() {}

    @Override
    @Suspendable
    public void call(FlowSession session){
        log.info("Pong.call() called");

        String message = session.receive(String.class);
        if(message.equals("ping")){
            log.info("Received Ping");
            session.send("pong");
        }

    }

}
