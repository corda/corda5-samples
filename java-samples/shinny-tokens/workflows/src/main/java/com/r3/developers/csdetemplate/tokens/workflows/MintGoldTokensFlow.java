package com.r3.developers.csdetemplate.tokens.workflows;

import com.r3.developers.csdetemplate.utxoexample.contracts.GoldContract;
import com.r3.developers.csdetemplate.utxoexample.states.GoldState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.FlowEngine;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.common.Party;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static net.corda.v5.crypto.DigestAlgorithmName.SHA2_256;

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
public class MintGoldTokensFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(MintGoldTokensFlow.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService ledgerService;

    @CordaInject
    public NotaryLookup notaryLookup;

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;


    @Suspendable
    @Override
    public String call( ClientRequestBody requestBody) {

        log.info("CreateNewChatFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            MintGoldFlowInputArgs mintGoldInputRequest = requestBody.getRequestBodyAs(jsonMarshallingService, MintGoldFlowInputArgs.class);

            // Get MemberInfos for the Vnode running the flow and the issuerMember.
            MemberInfo myInfo = memberLookup.myInfo();

            MemberInfo issuerMember = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(mintGoldInputRequest.getIssuer())),
                    "MemberLookup can't find issuerMember specified in flow arguments."
            );

            GoldState goldState = new GoldState(getSecureHash(issuerMember.getName().getCommonName()),
                    mintGoldInputRequest.getSymbol(),
                    new BigDecimal(mintGoldInputRequest.getValue()),
                    Arrays.asList(myInfo.getLedgerKeys().get(0)),
                    getSecureHash(myInfo.getName().getCommonName()));

            // Obtain the Notary name and public key.
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();
            PublicKey notaryKey = null;
            for(MemberInfo memberInfo: memberLookup.lookup()){
                if(Objects.equals(
                        memberInfo.getMemberProvidedContext().get("corda.notary.service.name"),
                        notary.getName().toString())) {
                    notaryKey = memberInfo.getLedgerKeys().get(0);
                    break;
                }
            }
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            if(notaryKey == null) {
                throw new CordaRuntimeException("No notary PublicKey found");
            }

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.getTransactionBuilder()
                    .setNotary(new Party(notary.getName(), notaryKey))
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(goldState)
                    .addCommand(new GoldContract.Create())
                    .addSignatories(myInfo.getLedgerKeys().get(0));

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            UtxoSignedTransaction finalizedSignedTransaction = ledgerService.finalize(
                    signedTransaction,
                    Arrays.asList()
            );
            String result = finalizedSignedTransaction.getId().toString();
            log.info("Success! Response: " + result);
            return result;
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }

    private SecureHash getSecureHash(String commonName) throws NoSuchAlgorithmException {
        return new SecureHash(SHA2_256.getName(), MessageDigest.getInstance(SHA2_256.getName()).digest(commonName.getBytes()));
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.MintGoldTokensFlow",
    "requestBody": {
        "symbol":"GOLD",
        "issuer":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "value":"20"
        }
}
 */
