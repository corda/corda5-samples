package com.r3.developers.pingpong.workflows;

import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InitiatingFlow(protocol = "ping")
public class Ping implements ClientStartableFlow {
    private final static Logger log = LoggerFactory.getLogger(Ping.class);

    // JsonMarshallingService provides a service for manipulating JSON.
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // FlowMessaging provides a service that establishes flow sessions between virtual nodes
    // that send and receive payloads between them.
    @CordaInject
    public FlowMessaging flowMessaging;

    // MemberLookup provides a service for looking up information about members of the virtual network which
    // this CorDapp operates in.
    @CordaInject
    public MemberLookup memberLookup;

    public Ping() {}

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody){
        log.info("Ping.call() called");

        PingFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, PingFlowArgs.class);

        // Obtain the MemberX500Name of the other member.
        MemberX500Name otherMember = flowArgs.getOtherMember();

        MemberInfo myInfo = memberLookup.myInfo();

        FlowSession session = flowMessaging.initiateFlow(otherMember);
        final String message = session.sendAndReceive(String.class, "ping");
        if(message.equals("pong")){
            log.info("Received Pong");
            return message;
        }

        return null;


}

}
