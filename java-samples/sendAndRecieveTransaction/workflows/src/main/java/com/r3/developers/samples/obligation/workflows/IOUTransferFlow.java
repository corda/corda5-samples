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

public class IOUTransferFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(IOUTransferFlow.class);

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
        log.info("IOUTransferFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            IOUTransferFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IOUTransferFlowArgs.class);

            // Get flow args from the input JSON
            UUID iouID = flowArgs.getIouID();

            //query the IOU input
            List<StateAndRef<IOUState>> iouStateAndRefs = ledgerService.findUnconsumedStatesByExactType(IOUState.class,100, Instant.now()).getResults();
            List<StateAndRef<IOUState>> iouStateAndRefsWithId = iouStateAndRefs.stream()
                    .filter(sar -> sar.getState().getContractState().getLinearId().equals(iouID)).collect(toList());
            if (iouStateAndRefsWithId.size() != 1) throw new CordaRuntimeException("Multiple or zero IOU states with id " + iouID + " found");
            StateAndRef<IOUState> iouStateAndRef = iouStateAndRefsWithId.get(0);
            IOUState iouInput = iouStateAndRef.getState().getContractState();

            //flow logic checks
            MemberInfo myInfo = memberLookup.myInfo();
            if (!(myInfo.getName().equals(iouInput.getLender()))) throw new CordaRuntimeException("Only IOU lender can transfer the IOU.");

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            MemberInfo newLenderInfo = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getNewLender())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );
            MemberInfo borrower = requireNonNull(
                    memberLookup.lookup(iouInput.getBorrower()),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            // Create the IOUState from the input arguments and member information.
            IOUState iouOutput = iouInput.withNewLender(newLenderInfo.getName(), Arrays.asList(borrower.getLedgerKeys().get(0), newLenderInfo.getLedgerKeys().get(0)));

            //get notary from input
            MemberX500Name notary = iouStateAndRef.getState().getNotaryName();

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(iouStateAndRef.getRef())
                    .addOutputState(iouOutput)
                    .addCommand(new IOUContract.Transfer())
                    .addSignatories(Arrays.asList(borrower.getLedgerKeys().get(0), newLenderInfo.getLedgerKeys().get(0),myInfo.getLedgerKeys().get(0)));

            // Convert the transaction builder to a UTXOSignedTransaction and sign with this Vnode's first Ledger key.
            // Note, toSignedTransaction() is currently a placeholder method, hence being marked as deprecated.
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(new FinalizeIOUFlow.FinalizeIOU(signedTransaction, Arrays.asList(borrower.getName(),newLenderInfo.getName())));
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
    "clientRequestId": "transferiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUTransferFlow",
    "requestBody": {
        "newLender":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "iouID":"1ac69d82-804b-487b-9178-ea527d0e4b80"
        }
}
 */