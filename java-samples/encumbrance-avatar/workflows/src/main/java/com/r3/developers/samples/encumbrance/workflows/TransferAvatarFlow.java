package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.contracts.AvatarContract;
import com.r3.developers.samples.encumbrance.contracts.ExpiryContract;
import com.r3.developers.samples.encumbrance.states.Avatar;
import com.r3.developers.samples.encumbrance.states.Expiry;
import com.r3.developers.samples.encumbrance.states.Member;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow(protocol = "transfer-avatar")
public class TransferAvatarFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(TransferAvatarFlow.class);
    @CordaInject
    private JsonMarshallingService jsonMarshallingService;
    @CordaInject
    private MemberLookup memberLookup;
    @CordaInject
    private NotaryLookup notaryLookup;
    @CordaInject
    private UtxoLedgerService ledgerService;
    @CordaInject
    private FlowMessaging flowMessaging;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        try {
            TransferAvatarFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TransferAvatarFlowArgs.class);
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();
            MemberInfo memberInfo = memberLookup.lookup(MemberX500Name.parse(flowArgs.getBuyer()));
            Member buyer = new Member(memberInfo.getName(), memberInfo.getLedgerKeys().get(0));

            // Fetch Avatar from the database
            List<StateAndRef<Avatar>> avatarStateAndRefs = ledgerService.findUnconsumedStatesByType(Avatar.class);
            List<StateAndRef<Avatar>> filteredAvatarStateAndRefs =
                    avatarStateAndRefs.stream().filter(avatarStateStateAndRef -> avatarStateStateAndRef.getState()
                                    .getContractState().getAvatarId().equals(flowArgs.getAvatarId()))
                            .collect(Collectors.toList());
            if (filteredAvatarStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Auction states with id " + flowArgs.getAvatarId() + " found");
            }
            StateAndRef<Avatar> avatarStateAndRef = filteredAvatarStateAndRefs.get(0);

            // Fetch Expiry from the database
            List<StateAndRef<Expiry>> expiryStateAndRefs = ledgerService.findUnconsumedStatesByType(Expiry.class);
            List<StateAndRef<Expiry>> filteredExpiryStateAndRefs =
                    expiryStateAndRefs.stream().filter(expiryStateStateAndRef -> expiryStateStateAndRef.getState()
                                    .getContractState().getAvatarId().equals(flowArgs.getAvatarId()))
                            .collect(Collectors.toList());
            if (filteredExpiryStateAndRefs.size() != 1) {
                throw new CordaRuntimeException(
                        "Multiple or zero Expiry states with id " + flowArgs.getAvatarId() + " found");
            }
            StateAndRef<Expiry> expiryStateAndRef = filteredExpiryStateAndRefs.get(0);

            // Change Owner of Avatar
            Avatar avatar = new Avatar(buyer, flowArgs.getAvatarId());
            Expiry expiry = new Expiry(
                    expiryStateAndRef.getState().getContractState().getExpiry(),
                    flowArgs.getAvatarId(), buyer);

            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                    .addInputStates(avatarStateAndRef.getRef(), expiryStateAndRef.getRef())
                    .addEncumberedOutputStates("avatar", avatar, expiry)
                    .addCommand(new AvatarContract.Transfer())
                    .addCommand(new ExpiryContract.Pass())
                    .addSignatories(buyer.getLedgerKey(),
                            avatarStateAndRef.getState().getContractState().getOwner().getLedgerKey());

            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();

            UtxoSignedTransaction finalizedSignedTransaction =
                    ledgerService.finalize(signedTransaction,
                            Collections.singletonList(flowMessaging.initiateFlow(buyer.getName()))).getTransaction();
            return finalizedSignedTransaction.getId().toString();
        }catch (Exception e){
            log.warn("Failed to process flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "transfer-avatar",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAvatarFlow",
  "requestBody": {
    "avatarId": "AVATAR-1321",
    "buyer": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
 */