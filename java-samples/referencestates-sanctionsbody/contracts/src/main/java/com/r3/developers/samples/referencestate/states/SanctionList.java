package com.r3.developers.samples.referencestate.states;

import com.r3.developers.samples.referencestate.contracts.SanctionListContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

/**
 * The states object recording list of untrusted parties.
 *
 * A states must implement [ContractState] or one of its descendants.
 *
 */
@BelongsToContract(SanctionListContract.class)
public class SanctionList implements ContractState {

    private List<Member> badPeople;
    private Member issuer;
    private UUID uniqueId;

    private List<PublicKey> participants;

    @ConstructorForDeserialization
    public SanctionList(List<Member> badPeople, Member issuer, UUID uniqueId, List<PublicKey> participants) {
        this.badPeople = badPeople;
        this.issuer = issuer;
        this.uniqueId = uniqueId;
        this.participants = participants;
    }

    public SanctionList(List<Member> badPeople, Member issuer, List<PublicKey> participants) {
        this(badPeople, issuer, UUID.randomUUID(), participants);
    }

    public List<Member> getBadPeople() {
        return badPeople;
    }

    public Member getIssuer() {
        return issuer;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }
}
