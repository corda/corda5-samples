package com.r3.developers.samples.primenumber.services


import com.r3.developers.samples.primenumber.Prime
import com.r3.developers.samples.primenumber.PrimeCommands
import com.r3.developers.samples.primenumber.workflows.QueryPrimeResponse
import com.r3.developers.samples.primenumber.workflows.ServiceSignResponse
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.transaction.filtered.UtxoFilteredData
import net.corda.v5.ledger.utxo.transaction.filtered.UtxoFilteredTransaction
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.PublicKey
import java.util.LinkedHashMap

class MaxSizeHashMap<K, V>(private val maxSize: Int = 1024) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>?) = size > maxSize
}

@CordaSerializable
class PrimeService() {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val EXPECTED_SINGLE_COMMAND = "Service expected a single PrimeCommands command from the filtered transaction."
        const val EXPECTED_SINGLE_OUTPUT_STATE = "Service expected a single Output State from the filtered transaction."
        const val EXPECTED_SINGLE_SIGNATORY = "Service expected to be the only signatory from filtered transaction."
    }

    private val cache = MaxSizeHashMap<Int,Int>()

    private val primes = generateSequence(1){ it + 1 }.filter { BigInteger.valueOf(it.toLong()).isProbablePrime(16) }


    fun queryNthPrime(n: Int, primeService: PrimeService): QueryPrimeResponse {
        require(n > 0) { "n must be at least one." }
        val nthPrime = cache[n] ?: run {
            val result = primes.take(n).last()
            cache[n] = result
            result
        }
        return QueryPrimeResponse(nthPrime,primeService)
    }

    fun yesOrNo(filteredTransaction: UtxoFilteredTransaction, primeService: PrimeService, requesterIdentity: PublicKey): ServiceSignResponse {

        //Checks if the filtered transaction's partial merkle tree is valid
        filteredTransaction.verify()

        //Checks if the filtered transaction passes the internal checks
        checkFilteredTransaction(filteredTransaction, primeService, requesterIdentity)

        return ServiceSignResponse(true)
    }

    private fun checkFilteredTransaction(filteredTransaction: UtxoFilteredTransaction, primeService: PrimeService, requesterIdentity: PublicKey): Boolean {
        //This function will return true if the filtered transaction:

        // - is only a Create Command
        val commands = filteredTransaction.commands.castOrThrow<UtxoFilteredData.Audit<Command>> { "Could not fetch command" }
        val command = commands.values.values.singleOrNull() ?: throw CordaRuntimeException(EXPECTED_SINGLE_COMMAND)
        "Not Create Command" using { command is PrimeCommands.Create }
        log.info("command: $command")

        // - states the correct prime (checked by local cache in this class)
        val filteredStateData = filteredTransaction.outputStateAndRefs.castOrThrow<UtxoFilteredData.Audit<StateAndRef<Prime>>> {
            "Could not fetch output state (Prime)"
        }
        val state = filteredStateData.values.values.singleOrNull()?.state ?: throw CordaRuntimeException(EXPECTED_SINGLE_OUTPUT_STATE)
        val nthPrimeFromState = state.contractState.nthPrime
        val nthPrimeFromCache = primeService.queryNthPrime(state.contractState.n,primeService).nthPrime
        "Nth Prime not correct" using { nthPrimeFromState == nthPrimeFromCache }
        log.info("nthPrimeFromState: $nthPrimeFromState || nthPrimeFromCache: $nthPrimeFromCache")

        // - lists this requesting vNode as a signer
        val signatories = filteredTransaction.signatories.castOrThrow<UtxoFilteredData.Audit<PublicKey>> {
            "Could not fetch signatories"
        }
        val signatory = signatories.values.values.lastOrNull() ?: throw CordaRuntimeException(EXPECTED_SINGLE_SIGNATORY)
        "FAIL" using { signatories.values.values.size == 1 && signatory == requesterIdentity }
        log.info("signatory: $signatory")

        return true
    }

    private inline fun <reified T> Any.castOrThrow(error: () -> String) = this as? T
        ?: throw CordaRuntimeException(error())

    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed Expectation: $this")
    }
}