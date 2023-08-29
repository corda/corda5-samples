package com.r3.developers.samples.persistence.schema

import net.corda.v5.base.annotations.CordaSerializable
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "INSURANCE_DETAIL")
@CordaSerializable
class PersistentInsurance(

    @Id
    @Column(name = "policy_number")
    val policyNumber: String,

    @Column(name = "insured_value")
    val insuredValue: Long,

    val duration: Int,

    val premium: Int,

    @OneToOne(cascade = [CascadeType.PERSIST])
    @JoinColumns(
        JoinColumn(name = "vehicleId    ", referencedColumnName = "vehicleid"),
        JoinColumn(name = "registrationNumber", referencedColumnName = "registration_number"))
    val vehicle: PersistentVehicle?,

    @OneToMany(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "policyNumber")
    val claims: List<PersistentClaim>
) {
    constructor() : this("", 0, 0, 0, PersistentVehicle(), listOf(PersistentClaim()))
}