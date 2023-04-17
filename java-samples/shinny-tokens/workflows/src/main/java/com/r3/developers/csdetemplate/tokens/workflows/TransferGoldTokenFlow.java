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
import net.corda.v5.ledger.utxo.token.selection.ClaimedToken;
import net.corda.v5.ledger.utxo.token.selection.TokenClaim;
import net.corda.v5.ledger.utxo.token.selection.TokenClaimCriteria;
import net.corda.v5.ledger.utxo.token.selection.TokenSelection;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static net.corda.v5.crypto.DigestAlgorithmName.SHA2_256;

public class TransferGoldTokenFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(TransferGoldTokenFlow.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    public NotaryLookup notaryLookup;

    @CordaInject
    public TokenSelection tokenSelection;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @CordaInject
    public FlowEngine flowEngine;

    @Suspendable
    @Override
    public String call( ClientRequestBody requestBody) {
        TokenClaim tokenClaim = null;
        BigDecimal totalAmount = null;
        BigDecimal change = null;

        try {

            TransferGoldFlowInputArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TransferGoldFlowInputArgs.class);

            MemberInfo myInfo = memberLookup.myInfo();

            MemberInfo newOwnerMember = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getNewOwner())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            MemberInfo issuerMember = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getIssuer())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

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

            if(notaryKey == null) {
                throw new CordaRuntimeException("No notary PublicKey found");
            }

            TokenClaimCriteria tokenClaimCriteria = new TokenClaimCriteria(
                    GoldState.class.getName(),
                    getSecureHash(issuerMember.getName().getCommonName()),
                    notary.getName(),
                    flowArgs.getSymbol(),
                    new BigDecimal(flowArgs.getValue())
            );

            tokenClaim = tokenSelection.tryClaim(tokenClaimCriteria);

            if(tokenClaim == null) {
                log.info("No tokens found for" + jsonMarshallingService.format(tokenClaimCriteria));
                return "No Tokens Found";
            }

            List<ClaimedToken> claimedTokenList = tokenClaim.getClaimedTokens().stream().collect(Collectors.toList());

            totalAmount = claimedTokenList.stream().map(ClaimedToken::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            change = totalAmount.subtract(new BigDecimal(flowArgs.getValue()));

            log.info("Found total " + totalAmount + " amount of tokens for " + jsonMarshallingService.format(tokenClaimCriteria));

            GoldState goldStateNew = new GoldState(getSecureHash(issuerMember.getName().getCommonName()),
                    flowArgs.getSymbol(), new BigDecimal(flowArgs.getValue()),
                    Arrays.asList(newOwnerMember.getLedgerKeys().get(0)),
                    getSecureHash(newOwnerMember.getName().getCommonName()));

            UtxoTransactionBuilder txBuilder = null;

            if(change.compareTo(BigDecimal.ZERO) > 0) {
                GoldState goldStateChange = new GoldState(getSecureHash(issuerMember.getName().getCommonName()),
                        flowArgs.getSymbol(), change,
                        Arrays.asList(myInfo.getLedgerKeys().get(0)),
                        getSecureHash(newOwnerMember.getName().getCommonName()));

                txBuilder = ledgerService.getTransactionBuilder()
                        .setNotary(new Party(notary.getName(), notaryKey))
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()))
                        .addOutputStates(Arrays.asList(goldStateChange, goldStateNew))
                        .addCommand(new GoldContract.Transfer())
                        .addSignatories(Arrays.asList(myInfo.getLedgerKeys().get(0), newOwnerMember.getLedgerKeys().get(0)));
            } else {
                txBuilder = ledgerService.getTransactionBuilder()
                        .setNotary(new Party(notary.getName(), notaryKey))
                        .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                        .addInputStates(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()))
                        .addOutputStates(goldStateNew)
                        .addCommand(new GoldContract.Transfer())
                        .addSignatories(Arrays.asList(myInfo.getLedgerKeys().get(0), newOwnerMember.getLedgerKeys().get(0)));
            }

            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

           // flowEngine.subFlow(new FinalizeMintSubFlow(signedTransaction, issuerMember.getName()));
            // calling the above gives an exception
            flowEngine.subFlow(new FinalizeMintSubFlow(signedTransaction, newOwnerMember.getName()));

        }
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());

            log.info("Released the claim on the token states, indicating we spent none of them");

            tokenClaim.useAndRelease(Arrays.asList());

            throw new CordaRuntimeException(e.getMessage());
        }
        finally {
            if(tokenClaim != null) {
                log.info("Release the claim on the token states, indicating we spent them all");

                tokenClaim.useAndRelease(tokenClaim.getClaimedTokens().stream().map(ClaimedToken::getStateRef).collect(Collectors.toList()));

                return "Total Available amount of Tokens : " + totalAmount + " change to be given back to the owner : " + change + " Total amount satisfied " + totalAmount.subtract(change);

            }
            return "No Tokens Found";
        }
    }

    private SecureHash getSecureHash(String commonName) throws NoSuchAlgorithmException {
        return new SecureHash(SHA2_256.getName(), MessageDigest.getInstance(SHA2_256.getName()).digest(commonName.getBytes()));
    }
}

/*
{
    "clientRequestId": "transfer-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.TransferGoldTokenFlow",
    "requestBody": {
        "symbol":"GOLD",
        "issuer":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "newOwner":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "value": "5"
        }
}
 */