package com.r3.developers.samples.obligation.workflows;

import net.corda.v5.application.crypto.DigestService;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.utxo.StateRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "utxo-transaction-transmission-protocol")
public class sendAndReceiveTransaction implements ClientStartableFlow {

    @CordaInject
    private FlowMessaging flowMessaging;

    @CordaInject
    private UtxoLedgerService utxoLedgerService;

    @CordaInject
    private JsonMarshallingService jsonMarshallingService;

    @CordaInject
    private MemberLookup memberLookup;

    @CordaInject
    private DigestService digestService;

    private static final Logger log = LoggerFactory.getLogger(sendAndReceiveTransaction.class);

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {
        sendAndRecieveTransactionArgs request = requestBody.getRequestBodyAs(jsonMarshallingService, sendAndRecieveTransactionArgs.class);

        // Parse the state reference to obtain the transaction ID.
        SecureHash transactionId = StateRef.parse(request.getStateRef() + ":0", digestService).getTransactionId();

        // Retrieve the signed transaction from the ledger.
        var transaction = requireNotNull(utxoLedgerService.findSignedTransaction(transactionId),
                "Transaction is not found or verified.");

        // Map the X500 names in the request to Member objects, ensuring each member exists.
        var members = request.getMembers().stream()
                .map(x500 -> requireNotNull(memberLookup.lookup(MemberX500Name.parse(x500)),
                        "Member " + x500 + " does not exist in the membership group"))
                .collect(Collectors.toList());

        // Initialize the sessions with the memebers that will be used to send the transaction.
        var sessions = members.stream()
                .map(member -> flowMessaging.initiateFlow(member.getName()))
                .collect(Collectors.toList());

        // Send the transaction with or without backchain depending on the request.
        try {
            if (request.isForceBackchain()) {
                utxoLedgerService.sendTransactionWithBackchain(transaction, sessions);
            } else {
                utxoLedgerService.sendTransaction(transaction, sessions);
            }
        } catch (Exception e) {
            // Log and rethrow any exceptions encountered during transaction sending.
            log.warn("Sending transaction for " + transactionId + " failed.", e);
            throw e;
        }

        // Format and log the successful transaction response.
        String response = jsonMarshallingService.format(transactionId.toString());
        log.info("SendTransaction is successful. Response: " + response);
        return response;
    }

    private <T> T requireNotNull(T obj, String message) {
        if (obj == null) throw new IllegalArgumentException(message);
        return obj;
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "sendAndRecieve-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.sendAndReceiveTransaction",
    "requestBody": {
        "stateRef": "STATE REF ID HERE",
        "members": ["CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"],
        "forceBackchain": "false"
    }
}
*/
