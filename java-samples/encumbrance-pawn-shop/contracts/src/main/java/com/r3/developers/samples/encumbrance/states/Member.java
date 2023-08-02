package com.r3.developers.samples.encumbrance.states;

import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

import java.security.PublicKey;

/**
 * This class encompasses a participants X500 name and its key. This is used in the contract to conveniently
 * get the key corresponding to a participant, so it can be checked for required signatures.
 */
@CordaSerializable
public class Member {
    private MemberX500Name name;
    private PublicKey ledgerKey;

    public Member() {
    }

    @ConstructorForDeserialization
    public Member(MemberX500Name name, PublicKey ledgerKey) {
        this.name = name;
        this.ledgerKey = ledgerKey;
    }

    public MemberX500Name getName() {
        return name;
    }

    public void setName(MemberX500Name name) {
        this.name = name;
    }

    public PublicKey getLedgerKey() {
        return ledgerKey;
    }

    public void setLedgerKey(PublicKey ledgerKey) {
        this.ledgerKey = ledgerKey;
    }

    @Override
    public String toString() {
        return "Member{" +
                "name=" + name +
                '}';
    }
}

