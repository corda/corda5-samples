package com.r3.developers.samples.persistence.states

import net.corda.v5.base.annotations.CordaSerializable

/**
 * Simple POJO class for the vehicle details.
 */
@CordaSerializable
data class VehicleDetail(val registrationNumber: String,
                         val chasisNumber: String,
                         val make: String,
                         val model: String,
                         val variant: String,
                         val color: String,
                         val fuelType: String)