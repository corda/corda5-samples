package com.r3.developers.samples.tokens.workflows;

import com.r3.developers.samples.tokens.contracts.GoldContract;
import com.r3.developers.samples.tokens.states.GoldState;
import net.corda.v5.application.crypto.DigestService;
import net.corda.v5.application.flows.*;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static java.util.Objects.requireNonNull;
import static net.corda.v5.crypto.DigestAlgorithmName.SHA2_256;

// Alice will trigger this flow to issue gold tokens to Bob.
public class IssueGoldTokensFlow implements ClientStartableFlow {
    private final static Logger log = LoggerFactory.getLogger(IssueGoldTokensFlow.class);
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;
    @CordaInject
    public MemberLookup memberLookup;
    @CordaInject
    public UtxoLedgerService ledgerService;
    @CordaInject
    public NotaryLookup notaryLookup;
    @CordaInject
    public FlowEngine flowEngine;
    @CordaInject
    public DigestService digestService;

    @NotNull
    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            IssueGoldTokenFlowArgs mintGoldInputRequest =
                    requestBody.getRequestBodyAs(jsonMarshallingService, IssueGoldTokenFlowArgs.class);

            // Get MemberInfos for the Vnode running the flow and the issuerMember.
            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo owner = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(mintGoldInputRequest.getOwner())),
                    "MemberLookup can't find owner specified in flow arguments."
            );
            // Obtain the Notary
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

            GoldState goldState = new GoldState(
                    getSecureHash(myInfo.getName().getCommonName()),
                    getSecureHash(owner.getName().getCommonName()),
                    mintGoldInputRequest.getSymbol(),
                    new BigDecimal(mintGoldInputRequest.getAmount()),
                    Collections.singletonList(owner.getLedgerKeys().get(0))
            );


            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(goldState)
                    .addCommand(new GoldContract.Issue())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();


            return flowEngine.subFlow(new FinalizeMintSubFlow(signedTransaction, owner.getName()));
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }

    @Suspendable
    private SecureHash getSecureHash(String commonName) {
        return digestService.hash(commonName.getBytes(), SHA2_256);
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "mint-1",
    "flowClassName": "com.r3.developers.samples.tokens.workflows.IssueGoldTokensFlow",
    "requestBody": {
        "symbol": "GOLD",
        "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "amount": "20"
    }
}
 */
