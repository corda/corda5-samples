package com.r3.developers.samples.negotiation.util

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import java.security.PublicKey

/**
 * This class encompasses a participants X500 name and its key. This is used in the contract to conveniently
 * get the key corresponding to a participant, so it can be checked for required signatures.
 */
@CordaSerializable
class Member(
    val name: MemberX500Name,
    val ledgerKey: PublicKey
) {
    override fun toString(): String {
        return "Member{name=$name}"
    }
}