package com.r3.developers.samples.persistence.schema

import net.corda.v5.base.annotations.CordaSerializable
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "VEHICLE_DETAIL")
@CordaSerializable
class PersistentVehicle(
    @Id
    val vehicleId: UUID,

    @Column(name = "registration_number")
    val registrationNumber: String,

    @Column(name = "chasis_number")
    val chasisNumber: String,

    val make: String,

    val model: String,

    val variant: String,

    val color: String,

    @Column(name = "fuel_type")
    val fuelType: String
) {
    // Default constructor required by hibernate.
    constructor(registrationNumber: String, chasisNumber: String, make: String, model: String, variant: String,
                color: String, fuelType: String) : this(
        UUID.randomUUID(), registrationNumber, chasisNumber, make, model, variant, color, fuelType)

    constructor() : this(UUID.randomUUID(), "", "", "", "", "", "", "")
}