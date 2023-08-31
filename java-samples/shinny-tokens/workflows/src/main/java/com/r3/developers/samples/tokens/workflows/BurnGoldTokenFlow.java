package com.r3.developers.samples.tokens.workflows;

import com.r3.developers.samples.tokens.contracts.GoldContract;
import com.r3.developers.samples.tokens.states.GoldState;
import net.corda.v5.application.crypto.DigestService;
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
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.token.selection.ClaimedToken;
import net.corda.v5.ledger.utxo.token.selection.TokenClaim;
import net.corda.v5.ledger.utxo.token.selection.TokenClaimCriteria;
import net.corda.v5.ledger.utxo.token.selection.TokenSelection;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static net.corda.v5.crypto.DigestAlgorithmName.SHA2_256;

public class BurnGoldTokenFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(BurnGoldTokenFlow.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    public NotaryLookup notaryLookup;

    // Token Selection API can be injected with CordaInject
    @CordaInject
    public TokenSelection tokenSelection;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @CordaInject
    public DigestService digestService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        TokenClaim tokenClaim = null;
        BigDecimal totalAmount = null;
        BigDecimal change = null;

        try{
            BurnGoldTokenFLowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, BurnGoldTokenFLowArgs.class);

            MemberInfo myInfo = memberLookup.myInfo();

            // Get the issuer of the token
            MemberInfo issuerMember = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getIssuer())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

            // Create the token claim criteria by specifying the issuer and amount
            TokenClaimCriteria tokenClaimCriteria = new TokenClaimCriteria(
                    GoldState.class.getName(),
                    getSecureHash(issuerMember.getName().getCommonName()),
                    notary.getName(),
                    flowArgs.getSymbol(),
                    new BigDecimal(flowArgs.getAmount())
            );

            // tryClaim will check in the vault if there are tokens which can satisfy the expected amount.
            // If yes all the fungible tokens are returned back.
            // Remaining change will be returned back to the sender.
            tokenClaim = tokenSelection.tryClaim(tokenClaimCriteria);

            if(tokenClaim == null) {
                log.info("No tokens found for" + jsonMarshallingService.format(tokenClaimCriteria));
                return "No Tokens Found";
            }

            List<ClaimedToken> claimedTokenList = tokenClaim.getClaimedTokens().stream().collect(Collectors.toList());

            // calculate the change to be given back to the sender
            totalAmount = claimedTokenList.stream().map(ClaimedToken::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            change = totalAmount.subtract(new BigDecimal(flowArgs.getAmount()));

            log.info("Found total " + totalAmount + " amount of tokens for " + jsonMarshallingService.format(tokenClaimCriteria));

            UtxoTransactionBuilder txBuilder = null;


            if(change.compareTo(BigDecimal.ZERO) > 0) {
                // if there is change to be returned back to the sender, create a new gold state representing the original
                // sender and the change.
                GoldState goldStateChange = new GoldState(
                        getSecureHash(issuerMember.getName().getCommonName()),
                        getSecureHash(myInfo.getName().getCommonName()),
                        flowArgs.getSymbol(), change,
                        Collections.singletonList(myInfo.getLedgerKeys().get(0))
                );

                txBuilder = ledgerService.createTransactionBuilder()
                        .setNotary(notary.getName())
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()))
                        .addOutputStates(List.of(goldStateChange))
                        .addCommand(new GoldContract.Burn())
                        .addSignatories(Collections.singletonList(myInfo.getLedgerKeys().get(0)));
            } else {
                // if there is no change, no need to create state representing the change to be given back to the sender.
                txBuilder = ledgerService.createTransactionBuilder()
                        .setNotary(notary.getName())
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()))
                        .addCommand(new GoldContract.Burn())
                        .addSignatories(Collections.singletonList(myInfo.getLedgerKeys().get(0)));
            }

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();
            UtxoSignedTransaction finalizedSignedTransaction = ledgerService.finalize(
                    signedTransaction,
                    Arrays.asList()
            ).getTransaction();

            String result = finalizedSignedTransaction.getId().toString();
            log.info("Success! Response: " + result);

        }catch (Exception e){
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());

            log.info("Released the claim on the token states, indicating we spent none of them");
            // None of the tokens were used, so release all the claimed tokens
            tokenClaim.useAndRelease(Arrays.asList());

            throw new CordaRuntimeException(e.getMessage());
        }finally {
            // Remove any used tokens from the cache and unlocks any remaining tokens for other flows to claim.
            if(tokenClaim != null) {
                log.info("Release the claim on the token states, indicating we spent them all");

                tokenClaim.useAndRelease(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()));

                return "Total Available amount of Tokens before burn action: " + totalAmount +", Amount burned: " + totalAmount.subtract(change)+", Change to be given back to the owner: " + change;

            }
            return "No Tokens Found";
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
    "clientRequestId": "burn-1",
    "flowClassName": "com.r3.developers.samples.tokens.workflows.BurnGoldTokenFlow",
    "requestBody": {
        "symbol": "GOLD",
        "issuer": "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB",
        "amount": "5"
        }
}
 */

