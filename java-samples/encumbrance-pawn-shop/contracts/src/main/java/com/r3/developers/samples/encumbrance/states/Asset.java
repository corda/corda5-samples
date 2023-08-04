package com.r3.developers.samples.encumbrance.states;

import com.r3.developers.samples.encumbrance.contracts.AssetContract;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

/**
 * A simple asset in Corda which can be used as a collateral for applying for a loan
 */
@BelongsToContract(AssetContract.class)
public class Asset implements ContractState {

    private final Member owner;
    private final String assetName;
    private final String assetId;
    private final  List<PublicKey> participants;

    public Asset(Member owner, String assetName, String assetId, List<PublicKey> participants) {
        this.owner = owner;
        this.assetName = assetName;
        this.assetId = assetId;
        this.participants = participants;
    }

    public Member getOwner() {
        return owner;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getAssetId() {
        return assetId;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }
}
