package com.r3.developers.samples.obligation.workflows;


import com.r3.developers.samples.obligation.contracts.IOUContract;
import com.r3.developers.samples.obligation.states.IOUState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.FlowEngine;

import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class IOUSettleFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(IOUSettleFlow.class);

    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the MemberLookup to look up the VNode identities.
    @CordaInject
    public MemberLookup memberLookup;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService ledgerService;

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;

    @Override
    @Suspendable
    public String call(ClientRequestBody requestBody) {
        log.info("IOUSettleFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            IOUSettleFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IOUSettleFlowArgs.class);

            // Get flow args from the input JSON
            UUID iouID = flowArgs.getIouID();
            int amountSettle = Integer.parseInt(flowArgs.getAmountSettle());

            //query the IOU input
            List<StateAndRef<IOUState>> iouStateAndRefs = ledgerService.findUnconsumedStatesByExactType(IOUState.class,100, Instant.now()).getResults();
            List<StateAndRef<IOUState>> iouStateAndRefsWithId = iouStateAndRefs.stream()
                    .filter(sar -> sar.getState().getContractState().getLinearId().equals(iouID)).collect(toList());

            if (iouStateAndRefsWithId.size() != 1) throw new CordaRuntimeException("Multiple or zero IOU states with id " + iouID + " found");
            StateAndRef<IOUState> iouStateAndRef = iouStateAndRefsWithId.get(0);
            IOUState iouInput = iouStateAndRef.getState().getContractState();

            //flow logic checks
            MemberInfo myInfo = memberLookup.myInfo();
            if (!(myInfo.getName().equals(iouInput.getBorrower()))) throw new CordaRuntimeException("Only IOU borrower can settle the IOU.");
            MemberInfo lenderInfo = requireNonNull(
                    memberLookup.lookup(iouInput.getLender()),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            // Create the IOUState from the input arguments and member information.
            IOUState iouOutput = iouInput.pay(amountSettle);

            //get notary from input
            MemberX500Name notary = iouStateAndRef.getState().getNotaryName();

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(iouStateAndRef.getRef())
                    .addOutputState(iouOutput)
                    .addCommand(new IOUContract.Settle())
                    .addSignatories(iouOutput.getParticipants());

            // Convert the transaction builder to a UTXOSignedTransaction and sign with this Vnode's first Ledger key.
            // Note, toSignedTransaction() is currently a placeholder method, hence being marked as deprecated.
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(new FinalizeIOUFlow.FinalizeIOU(signedTransaction, Arrays.asList(lenderInfo.getName())));
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "settleiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUSettleFlow",
    "requestBody": {
        "amountSettle":"10",
        "iouID":" ** fill in id **"
    }
}
1ac69d82-804b-487b-9178-ea527d0e4b80
*/


