package com.r3.developers.samples.encumbrance.workflows;

import com.r3.developers.samples.encumbrance.states.Avatar;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class GetAvatarFlow implements ClientStartableFlow {

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        List<AvatarDetail> iouList =
                ledgerService.findUnconsumedStatesByType(Avatar.class).stream().map(
                        it -> new AvatarDetail(
                                it.getState().getContractState().getOwner().getName(),
                                it.getState().getContractState().getAvatarId(),
                                it.getState().getEncumbranceGroup().getTag(),
                                it.getState().getEncumbranceGroup().getSize()
                        )
                ).collect(Collectors.toList());
        return jsonMarshallingService.format(iouList);
    }

    class AvatarDetail{
        private MemberX500Name owner;
        private String avatarId;
        private String encumbranceGroupTag;
        private int encumbranceGroupSize;

        public AvatarDetail(MemberX500Name owner, String avatarId, String encumbranceGroupTag, int encumbranceGroupSize) {
            this.owner = owner;
            this.avatarId = avatarId;
            this.encumbranceGroupTag = encumbranceGroupTag;
            this.encumbranceGroupSize = encumbranceGroupSize;
        }

        public MemberX500Name getOwner() {
            return owner;
        }

        public void setOwner(MemberX500Name owner) {
            this.owner = owner;
        }

        public String getAvatarId() {
            return avatarId;
        }

        public void setAvatarId(String avatarId) {
            this.avatarId = avatarId;
        }

        public String getEncumbranceGroupTag() {
            return encumbranceGroupTag;
        }

        public void setEncumbranceGroupTag(String encumbranceGroupTag) {
            this.encumbranceGroupTag = encumbranceGroupTag;
        }

        public int getEncumbranceGroupSize() {
            return encumbranceGroupSize;
        }

        public void setEncumbranceGroupSize(int encumbranceGroupSize) {
            this.encumbranceGroupSize = encumbranceGroupSize;
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "get-avatar",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.GetAvatarFlow",
  "requestBody": {
  }
}
 */