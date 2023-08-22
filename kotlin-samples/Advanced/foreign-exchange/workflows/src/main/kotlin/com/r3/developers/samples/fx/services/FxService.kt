package com.r3.developers.samples.fx.services

import net.corda.v5.base.exceptions.CordaRuntimeException
import org.slf4j.LoggerFactory

class FxService() {

    private companion object{
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val INVALID_CURRENCY_PAIR = "Invalid currencyPair given"
    }

    fun quoteFxRate(currencyPair: String): Float {
        "$INVALID_CURRENCY_PAIR: '$currencyPair'" using { enumContains<conversionRateTable>(currencyPair) }
        val conversionRate = conversionRateTable.valueOf(currencyPair).conversionRate
        log.info("currencyPair:$currencyPair | conversionRate: $conversionRate")
        return conversionRate
    }




    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("[FxService] Failed Expectation: $this")
    }

    private inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
        return enumValues<T>().any { it.name == name}
    }

}

/**
Please note: currently, it is difficult to create HTTP requests inside the sandbox environment.
So, rather than querying for conversion rates via an external source, we are simply hardcoding a small set of currencies
It is not impossible, but it is a separate advanced topic that merits its own sample or be an extended sample of this
Stay tuned with update notes to see when easy external messaging within the sandbox environment will be released!
 **/
//convetion for currencyPairing: "[currency_from][currency_to]"
enum class conversionRateTable(val conversionRate: Float) {
    GBPGBP(1f),
    GBPEUR(1.16f),
    GBPUSD(1.27f),
    GBPCAD(1.72f),
    EURGBP(0.85f),
    EUREUR(1f),
    EURUSD(1.08f),
    EURCAD(1.47f),
    USDGBP(0.78f),
    USDEUR(0.91f),
    USDUSD(1f),
    USDCAD(1.35f),
    CADGBP(0.58f),
    CADEUR(0.67f),
    CADUSD(0.73f),
    CADCAD(1f)

}