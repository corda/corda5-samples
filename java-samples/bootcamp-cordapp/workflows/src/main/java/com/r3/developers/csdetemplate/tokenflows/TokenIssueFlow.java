package com.r3.developers.csdetemplate.tokenflows;

import com.r3.developers.csdetemplate.utxoexample.contracts.TokenContract;
import com.r3.developers.csdetemplate.utxoexample.states.TokenState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@InitiatingFlow(protocol = "finalize-token-protocol")
public class TokenIssueFlow implements ClientStartableFlow {
    private final static Logger log = LoggerFactory.getLogger(TokenIssueFlow.class);

    //Marshal/Unmarshal from Json to DTO and DTO to Json
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;

    //Lookup other members on the network, lookup myInfo
    @CordaInject
    private MemberLookup memberLookup;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    private UtxoLedgerService ledgerService;

    //Access to notary
    @CordaInject
    private NotaryLookup notaryLookup;

    //initiate communication with counterparties
    @CordaInject
    private FlowMessaging flowMessaging;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        log.info("TokenIssueFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            TokenIssueFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TokenIssueFlowArgs.class);

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo ownerInfo = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getOwner())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            TokenState tokenState = new TokenState(
                    Integer.parseInt(flowArgs.getAmount()),
                    myInfo.getName(),
                    ownerInfo.getName(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), ownerInfo.getLedgerKeys().get(0))
            );

            // Obtain the Notary name and public key.
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(tokenState)
                    .addCommand(new TokenContract.Issue())
                    .addSignatories(tokenState.getParticipants());

            // Convert the transaction builder to a UTXOSignedTransaction and sign with this Vnode's first Ledger key.
            // Note, toSignedTransaction() is currently a placeholder method, hence being marked as deprecated.
            @SuppressWarnings("DEPRECATION")

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Call FinalizeTokenSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            FlowSession session = flowMessaging.initiateFlow(ownerInfo.getName());
            String result;
            try {
//Verifies, signs, collects signatures, records and broadcasts a UtxoSignedTransaction to involved peers.
                UtxoSignedTransaction finalizedSignedTransaction = ledgerService.finalize(
                        signedTransaction,
                        Arrays.asList(session)
                ).getTransaction();

                result = finalizedSignedTransaction.getId().toString();
                log.info("Success! Response: " + result);

            }
            // Soft fails the flow and returns the error message without throwing a flow exception.
            catch (Exception e) {
                log.warn("Finality failed", e);
                result = "Finality failed, " + e.getMessage();
            }
            // Returns the transaction id converted as a string
            return result;
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
    "clientRequestId": "createtoken-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokenflows.TokenIssueFlow",
    "requestBody": {
        "amount":"101",
        "owner":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */
