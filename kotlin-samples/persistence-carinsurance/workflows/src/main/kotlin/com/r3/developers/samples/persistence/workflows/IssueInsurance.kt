package com.r3.developers.samples.persistence.workflows

import com.r3.developers.samples.persistence.contracts.InsuranceContract
import com.r3.developers.samples.persistence.schema.PersistentInsurance
import com.r3.developers.samples.persistence.schema.PersistentVehicle
import com.r3.developers.samples.persistence.states.InsuranceState
import com.r3.developers.samples.persistence.states.VehicleDetail
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.membership.MemberInfo
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Duration
import java.time.Instant

@InitiatingFlow(protocol = "issue-insurance")
class IssueInsuranceFlow : ClientStartableFlow {

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

    // Injects the NotaryLookup to look up the notary identity.
    @CordaInject
    lateinit var notaryLookup: NotaryLookup

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
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IssueInsuranceFlowArgs::class.java)

            val myInfo = memberLookup.myInfo()
            val insuree = memberLookup.lookup(flowArgs.insuree)
                ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            val insurance = getInsuranceState(flowArgs, myInfo, insuree.ledgerKeys[0])

            // Obtain the notary
            val notary = notaryLookup.notaryServices.iterator().next()

            // Build the transaction
            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(insurance)
                .addCommand(InsuranceContract.IssueInsurance())
                .addSignatories(myInfo.ledgerKeys[0])

            // Sign the transaction
            val signedTransaction =  txBuilder.toSignedTransaction()

            // Persist the state information in custom database tables
            val persistentInsurance = persistInsurance(insurance)

            // Send the entity to counterparty. They use it to persist the information at their end.
            val session = flowMessaging.initiateFlow(flowArgs.insuree)
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
    private fun getInsuranceState
                (flowArgs: IssueInsuranceFlowArgs, myInfo: MemberInfo, insureeKey: PublicKey) : InsuranceState{

        return InsuranceState(
            VehicleDetail(
                flowArgs.vehicleInfo.registrationNumber,
                flowArgs.vehicleInfo.chasisNumber,
                flowArgs.vehicleInfo.make,
                flowArgs.vehicleInfo.model,
                flowArgs.vehicleInfo.variant,
                flowArgs.vehicleInfo.color,
                flowArgs.vehicleInfo.fuelType
            ),
            flowArgs.policyNumber,
            flowArgs.insuredValue,
            flowArgs.duration,
            flowArgs.premium,
            myInfo.name,
            flowArgs.insuree,
            emptyList(),
            listOf(myInfo.ledgerKeys[0], insureeKey)

        )
    }

    @Suspendable
    private fun persistInsurance(insurance: InsuranceState) : PersistentInsurance {
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
            emptyList()
        )
        persistentService.persist(persistentInsurance)
        return persistentInsurance
    }

}

/* Example JSON to put into REST-API POST requestBody
{
  "clientRequestId": "issue-1",
  "flowClassName": "com.r3.developers.samples.persistence.workflows.IssueInsuranceFlow",
  "requestBody": {
    "vehicleInfo": {
        "registrationNumber": "MH7777",
        "chasisNumber": "CH8771",
        "make": "Hyundai",
        "model": "i20",
        "variant": "Asta",
        "color": "grey",
        "fuelType": "Petrol"
    },
    "policyNumber" : "P001",
    "insuredValue": 500000,
    "duration": 2,
    "premium": 20000,
    "insuree": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
  }
}
*/

@InitiatedBy(protocol = "issue-insurance")
class IssueInsuranceResponder: ResponderFlow{
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
data class IssueInsuranceFlowArgs(
    val vehicleInfo: VehicleInfo,
    val policyNumber: String,
    val insuredValue: Long,
    val duration: Int,
    val premium: Int,
    val insuree: MemberX500Name
)

data class VehicleInfo(
    val registrationNumber: String,
    val chasisNumber: String,
    val make: String,
    val model: String,
    val variant: String,
    val color: String,
    val fuelType: String
)