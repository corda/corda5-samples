package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.contracts.AvatarContract;
import com.r3.developers.samples.encumbrance.contracts.ExpiryContract;
import com.r3.developers.samples.encumbrance.states.Avatar;
import com.r3.developers.samples.encumbrance.states.Expiry;
import com.r3.developers.samples.encumbrance.states.Member;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.common.NotaryLookup;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;

public class CreateAvatarFlow implements ClientStartableFlow {

    private final static Logger log =  LoggerFactory.getLogger(CreateAvatarFlow.class);
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
            CreateAvatarFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateAvatarFlowArgs.class);
            MemberInfo myInfo = memberLookup.myInfo();
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

            Avatar avatar = new Avatar(new Member(myInfo.getName(), myInfo.getLedgerKeys().get(0)), flowArgs.getAvatarId());
            Expiry expiry = new Expiry(
                    Instant.now().plus(flowArgs.getExpiryAfterMinutes(), ChronoUnit.MINUTES),
                    flowArgs.getAvatarId(),
                    avatar.getOwner()
            );
            UtxoTransactionBuilder transactionBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.getName())
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
                    .addEncumberedOutputStates("avatar", avatar, expiry)
                    .addCommand(new AvatarContract.Create())
                    .addCommand(new ExpiryContract.Create())
                    .addSignatories(avatar.getOwner().getLedgerKey());
            UtxoSignedTransaction signedTransaction = transactionBuilder.toSignedTransaction();
            UtxoSignedTransaction finalizedTransaction =
                    ledgerService.finalize(signedTransaction, Collections.emptyList()).getTransaction();

            return finalizedTransaction.getId().toString();
        }catch (Exception e){
            log.warn("Failed to process flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "create-avatar",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.CreateAvatarFlow",
  "requestBody": {
    "avatarId": "AVATAR-1321",
    "expiryAfterMinutes": 10
  }
}
 */
