package com.r3.developers.samples.encumbrance.states;

import com.r3.developers.samples.encumbrance.contracts.AvatarContract;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Avatar can be thought of as any metaverse avatar which needs to be created and sold on at an exchange. This entity
 * has an id and owner associated with it. We will see how this avatar can only be sold within a certain time limit.
 */
@BelongsToContract(AvatarContract.class)
public class Avatar implements ContractState {
    private final Member owner;
    private final String avatarId;

    public Avatar(Member owner, String avatarId) {
        this.owner = owner;
        this.avatarId = avatarId;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return Collections.singletonList(owner.getLedgerKey());
    }

    public Member getOwner() {
        return owner;
    }

    public String getAvatarId() {
        return avatarId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avatar avatar = (Avatar) o;
        return avatarId.equals(avatar.avatarId);
    }


    @Override
    public int hashCode() {
        return Objects.hash(avatarId);
    }
}
