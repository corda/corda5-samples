package com.r3.developers.samples.referencestate.states;

import com.r3.developers.samples.referencestate.contracts.SanctionableIOUContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The states object recording IOU agreements between two parties.
 *
 * A states must implement [ContractState] or one of its descendants.
 *
 */
@BelongsToContract(SanctionableIOUContract.class)
public class SanctionableIOUState implements ContractState {

    private final int value;
    private final Member lender;
    private final Member borrower;
    private final UUID uniqueIdentifier;

    @ConstructorForDeserialization
    public SanctionableIOUState(int value, Member lender, Member borrower, UUID uniqueIdentifier) {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public SanctionableIOUState(int value, Member lender, Member borrower) {
        this(value, lender, borrower, UUID.randomUUID());
    }

    public int getValue() {
        return value;
    }

    public Member getLender() {
        return lender;
    }

    public Member getBorrower() {
        return borrower;
    }

    public UUID getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return Arrays.asList(lender.getLedgerKey(), borrower.getLedgerKey());
    }
}
