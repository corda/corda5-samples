package com.r3.developers.samples.referencestate.workflows;

import com.r3.developers.samples.referencestate.states.Member;
import com.r3.developers.samples.referencestate.states.SanctionList;
import com.r3.developers.samples.referencestate.states.SanctionableIOUState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetSanctionListFlow implements ClientStartableFlow {

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public UtxoLedgerService ledgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {

        List<SanctionListDetail> sanctionList =
                ledgerService.findUnconsumedStatesByType(SanctionList.class).stream().map(
                        it-> new SanctionListDetail(
                                it.getState().getContractState().getBadPeople().stream().map(Member::getName)
                                        .collect(Collectors.toList()),
                                it.getState().getContractState().getIssuer().getName(),
                                it.getState().getContractState().getUniqueId()
                                )
                ).collect(Collectors.toList());

        return jsonMarshallingService.format(sanctionList);
    }

    class SanctionListDetail{
        private List<MemberX500Name> badPeople;
        private MemberX500Name issuer;
        private UUID uniqueId;

        public SanctionListDetail(List<MemberX500Name> badPeople, MemberX500Name issuer, UUID uniqueId) {
            this.badPeople = badPeople;
            this.issuer = issuer;
            this.uniqueId = uniqueId;
        }

        public List<MemberX500Name> getBadPeople() {
            return badPeople;
        }

        public void setBadPeople(List<MemberX500Name> badPeople) {
            this.badPeople = badPeople;
        }

        public MemberX500Name getIssuer() {
            return issuer;
        }

        public void setIssuer(MemberX500Name issuer) {
            this.issuer = issuer;
        }

        public UUID getUniqueId() {
            return uniqueId;
        }

        public void setUniqueId(UUID uniqueId) {
            this.uniqueId = uniqueId;
        }
    }
}

/* Example JSON to put into REST-API POST requestBody
 {
    "clientRequestId": "get-sanction",
    "flowClassName": "com.r3.developers.samples.referencestate.workflows.GetSactionListFlow",
    "requestBody": {
    }
 }
 */