package com.r3.developers.samples.obligation.workflows;

import com.r3.developers.samples.obligation.workflows.FinalizeIOUFlow;
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
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.StateRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static java.util.stream.Collectors.toList;

public class TestContractFlow implements ClientStartableFlow {

    private static final Logger log = LoggerFactory.getLogger(TestContractFlow.class);

    @CordaInject
    private JsonMarshallingService jsonMarshallingService;

    @CordaInject
    private MemberLookup memberLookup;

    @CordaInject
    private UtxoLedgerService ledgerService;

    @CordaInject
    private NotaryLookup notaryLookup;

    @CordaInject
    private FlowEngine flowEngine;


    public StateRef inputStateRef;

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {
        Map<String, String> results = new HashMap<>();

        log.info("TestContractFlow.call() called");

        try {
            TestContractFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TestContractFlowArgs.class);

            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.getOtherMember()));
            if (otherMember == null) {
                throw new CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.");
            }
            // Obtain the Notary name and public key.
            NotaryInfo notary = notaryLookup.getNotaryServices().stream().findFirst().get();

            UUID iouStateId = null;
            // Create a well formed transaction with an output State which can be referenced
            // as an input StateRef in the tests
            try {
                IOUState iouState = new IOUState(
                        10,
                        myInfo.getName(),
                        otherMember.getName(),
                        3,
                        UUID.randomUUID(),
                        Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
                );

                iouStateId = iouState.getLinearId();

                var txBuilder = ledgerService.createTransactionBuilder()
                        .setNotary(notary.getName())
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addOutputState(iouState)
                        .addCommand(new IOUContract.Issue())
                        .addSignatories(iouState.getParticipants());

                var signedTransaction = txBuilder.toSignedTransaction();

                inputStateRef = new StateRef(signedTransaction.getId(), 0);
                flowEngine.subFlow(new FinalizeIOUFlow.FinalizeIOU(signedTransaction, Arrays.asList(otherMember.getName())));

            } catch (Exception e) {
                throw new CordaRuntimeException("Set up transaction could not be created because of exception: " + e.getMessage());
            }

            // *************   START TESTS ****************

            // Multiple Commands not permitted
            results.put("Multiple Commands not permitted", testMultipleCommandsNotPermitted(myInfo, otherMember, notary));

            // Only Two Participants
            results.put("Only Two Participants", testOnlyTwoParticipants(myInfo, otherMember, notary));

            // Settle needs only one output
            results.put("Settle needs only one output", testSettleNeedsOnlyOneOutput(myInfo, otherMember, notary));

            // Transfer needs only one output
            results.put("Transfer needs only one output", testTransferNeedsOnlyOneOutput(myInfo, otherMember, notary));

            // Issue needs only one output
            results.put("Issue needs only one output", testIssueNeedsOnlyOneOutput(myInfo, otherMember, notary));

            // Issue needs only one output
            results.put("Settle needs only one input", testSettleNeedsOnlyOneInput(myInfo, otherMember, notary));

            // Transfer needs only one output
            results.put("Transfer needs only one input", testTransferNeedsOnlyOneInput(myInfo, otherMember, notary));


            return results.toString();

        } catch (Exception e) {
            log.warn("Failed to process utxo flow for request body '" + requestBody + "' because: '" + e.getMessage() + "'");
            throw e;
        }
    }

    @Suspendable
    private String testMultipleCommandsNotPermitted(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            //create sample output state for the test
            IOUState iouState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            //build sample transaction to test
            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addInputState(inputStateRef)
                    .addCommand(new IOUContract.Issue())
                    .addCommand(new IOUContract.Transfer())
                    .addSignatories(iouState.getParticipants());

            //sign the test to check if it fails
            var signedTransaction = txBuilder.toSignedTransaction();

            //no error found. Contract failed.
            return "Fail";

            //catching the error to see if the contract passed
        } catch (Exception e) {
            //check if gotten the expected error
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("Require a single command")) {
                //expected error found so the contract passes the test
                return "Pass";
            } else {
                //different error thrown. Contract failed
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }

    @Suspendable
    private String testOnlyTwoParticipants(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Collections.singletonList(myInfo.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addCommand(new IOUContract.Issue())
                    .addSignatories(iouState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("only two participants")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }

    @Suspendable
    private String testSettleNeedsOnlyOneOutput(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addOutputState(iouState)
                    .addInputState(inputStateRef)
                    .addCommand(new IOUContract.Settle())
                    .addSignatories(iouState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("one output state")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }

    @Suspendable
    private String testTransferNeedsOnlyOneOutput(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouOutputState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouOutputState)
                    .addOutputState(iouOutputState)
                    .addInputState(inputStateRef)
                    .addCommand(new IOUContract.Transfer())
                    .addSignatories(iouOutputState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("one output state")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }

    @Suspendable
    private String testIssueNeedsOnlyOneOutput(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addOutputState(iouState)
                    .addCommand(new IOUContract.Issue())
                    .addSignatories(iouState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("one output state")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }

    }

    @Suspendable
    private String testSettleNeedsOnlyOneInput(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouOutputState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouOutputState)
                    .addCommand(new IOUContract.Transfer())
                    .addSignatories(iouOutputState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("one input")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }

    @Suspendable
    private String testTransferNeedsOnlyOneInput(MemberInfo myInfo, MemberInfo otherMember, NotaryInfo notary) {
        try {
            IOUState iouOutputState = new IOUState(
                    10,
                    myInfo.getName(),
                    otherMember.getName(),
                    3,
                    UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), otherMember.getLedgerKeys().get(0))
            );

            var txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouOutputState)
                    .addCommand(new IOUContract.Transfer())
                    .addSignatories(iouOutputState.getParticipants());

            var signedTransaction = txBuilder.toSignedTransaction();

            return "Fail";

        } catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No exception message";
            if (exceptionMessage.contains("one input")) {
                return "Pass";
            } else {
                return "Contract failed but with a different Exception: " + e.getMessage();
            }
        }
    }
}


/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "dummy-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.TestContractFlow",
    "requestBody": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
    }
}
*/