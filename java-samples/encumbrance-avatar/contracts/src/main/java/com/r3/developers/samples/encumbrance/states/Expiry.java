package com.r3.developers.samples.encumbrance.states;

import com.r3.developers.samples.encumbrance.contracts.ExpiryContract;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Expiry represents an expiry date beyond which the avatar cannot be sold. This is the encumbrance state which
 * encumbers the Avatar state.
 */
@BelongsToContract(ExpiryContract.class)
public class Expiry implements ContractState {

    private final Instant expiry;
    private final String avatarId;
    private final Member owner;

    public Expiry(Instant expiry, String avatarId, Member owner) {
        this.expiry = expiry;
        this.avatarId = avatarId;
        this.owner = owner;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public Member getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return Collections.singletonList(owner.getLedgerKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expiry expiry1 = (Expiry) o;
        return expiry.equals(expiry1.expiry) && avatarId.equals(expiry1.avatarId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiry, avatarId);
    }
}
