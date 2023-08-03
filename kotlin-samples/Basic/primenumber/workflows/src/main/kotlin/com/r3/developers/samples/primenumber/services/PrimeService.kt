package com.r3.developers.samples.primenumber.services

import java.math.BigInteger
import java.util.LinkedHashMap

class MaxSizeHashMap<K, V>(private val maxSize: Int = 1024) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>?) = size > maxSize
}

class PrimeService() { //SingletonSerializeAsToken

    private val cache = MaxSizeHashMap<Int,Int>()

    private val primes = generateSequence(1){ it + 1 }.filter { BigInteger.valueOf(it.toLong()).isProbablePrime(16) }

    fun queryNthPrime(n: Int): Int {
        return cache.get(n) ?: run {
            require(n > 0) { "n must be at least one." }
            val result = primes.take(n).last()
            cache.put(n, result)
            result
        }
    }
}