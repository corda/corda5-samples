package com.r3.developers.samples.persistence.schema

import net.corda.v5.base.annotations.CordaSerializable
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "CLAIM_DETAIL")
@CordaSerializable
class PersistentClaim (
    @Id
    val id: String,

    @Column(name = "claim_number")
    var claimNumber: String,

    @Column(name = "claim_description")
    var claimDescription: String,

    @Column(name = "claim_amount")
    var claimAmount: Int,

    @Column
    var policyNumber: String
){
    // Default constructor required by hibernate.
    constructor(claimNumber: String, claimDescription: String, claimAmount: Int, policyNumber: String) : this(
        UUID.randomUUID().toString(), claimNumber, claimDescription, claimAmount, policyNumber)

    constructor() : this(UUID.randomUUID().toString(), "", "", 0, "")
}