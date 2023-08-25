package com.r3.developers.samples.persistence.workflows

import com.r3.developers.samples.persistence.contracts.InsuranceContract
import com.r3.developers.samples.persistence.schema.PersistentClaim
import com.r3.developers.samples.persistence.schema.PersistentInsurance
import com.r3.developers.samples.persistence.schema.PersistentVehicle
import com.r3.developers.samples.persistence.states.Claim
import com.r3.developers.samples.persistence.states.InsuranceState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

@InitiatingFlow(protocol = "add-claim")
class InsuranceClaimFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the MemberLookup to look up the VNode identities.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    // FlowMessaging service is required to start flow sessions with counterparty
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // Injects the Persistence Service required for persistence
    @CordaInject
    lateinit var persistentService: PersistenceService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        try{
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, InsuranceClaimFlowArgs::class.java)
            val myInfo = memberLookup.myInfo()

            // Query the vault to fetch a list of all Insurance state, and filter the results based on the policyNumber
            // to fetch the desired Insurance state from the vault. This filtered state would be used as input to the
            // transaction.
            val filteredInsuranceStateAndRefs =
                ledgerService.findUnconsumedStatesByType(InsuranceState::class.java).filter {
                it.state.contractState.policyNumber.equals(flowArgs.policyNumber)
            }

            if(filteredInsuranceStateAndRefs.isEmpty()){
                throw CordaRuntimeException("Multiple or zero Insurance states with id "
                        + flowArgs.policyNumber + " found")
            }

            val input = filteredInsuranceStateAndRefs[0]

            // Create claims
            val claim = Claim(flowArgs.claimNumber, flowArgs.claimDescription, flowArgs.claimAmount)

            val claims = ArrayList<Claim>()
            claims.add(claim)
            for (item in input.state.contractState.claims) {
                claims.add(item)
            }

            //Create the output state
            val output = InsuranceState(
                input.state.contractState.vehicleDetail,
                input.state.contractState.policyNumber,
                input.state.contractState.insuredValue,
                input.state.contractState.duration,
                input.state.contractState.premium,
                input.state.contractState.insurer,
                input.state.contractState.insuree,
                claims,
                input.state.contractState.participants
            )

            //Build the transaction
            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(input.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputState(input.ref)
                .addOutputState(output)
                .addCommand(InsuranceContract.AddClaim())
                .addSignatories(myInfo.ledgerKeys[0])

            // Sign the transaction
            val signedTransaction = txBuilder.toSignedTransaction()

            // Persist in custom database table
            val persistentInsurance = persistInsurance(output, claims)

            // Send the entity to counterparty. They use it to persist the information at their end.
            val session = flowMessaging.initiateFlow(input.state.contractState.insurer)
            session.send(persistentInsurance)

            // Calls the Corda provided finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            val finalizedTransaction =  ledgerService.finalize(signedTransaction, listOf(session)).transaction

            // Returns the transaction id converted to a string.
            return finalizedTransaction.id.toString()

        }catch(e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

    @Suspendable
    private fun persistInsurance(insurance: InsuranceState, claims: List<Claim>) : PersistentInsurance{
        val persistentClaims = ArrayList<PersistentClaim>()
        if(claims.isNotEmpty()){
            claims.forEach {
                val persistentClaim = PersistentClaim(
                    it.claimNumber,
                    it.claimDescription,
                    it.claimAmount,
                    insurance.policyNumber,
                )
                persistentClaims.add(persistentClaim)
            }
        }

        val persistentInsurance = PersistentInsurance(
            insurance.policyNumber,
            insurance.insuredValue,
            insurance.duration,
            insurance.premium,
            PersistentVehicle(
                insurance.vehicleDetail.registrationNumber,
                insurance.vehicleDetail.chasisNumber,
                insurance.vehicleDetail.make,
                insurance.vehicleDetail.model,
                insurance.vehicleDetail.variant,
                insurance.vehicleDetail.color,
                insurance.vehicleDetail.fuelType
            ),
            persistentClaims
        )

        persistentService.persist(persistentInsurance)
        return persistentInsurance;
    }

}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "claim-1",
  "flowClassName": "com.r3.developers.samples.persistence.workflows.InsuranceClaimFlow",
  "requestBody": {
    "policyNumber" : "P001",
    "claimNumber": "CM001",
    "claimDescription": "Simple Claim",
    "claimAmount": 50000
  }
}
*/


@InitiatedBy(protocol = "add-claim")
class InsuranceClaimFlowResponder: ResponderFlow {
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService
    @CordaInject
    lateinit var persistentService: PersistenceService

    @Suspendable
    override fun call(session: FlowSession) {
        val persistentInsurance = session.receive(PersistentInsurance::class.java)
        persistentService.persist(persistentInsurance)
        ledgerService.receiveFinality(session, {})
    }

}


// A class to hold the deserialized arguments required to start the flow.
data class InsuranceClaimFlowArgs(
    val claimNumber: String,
    val claimDescription: String,
    val claimAmount: Int,
    val policyNumber: String
)