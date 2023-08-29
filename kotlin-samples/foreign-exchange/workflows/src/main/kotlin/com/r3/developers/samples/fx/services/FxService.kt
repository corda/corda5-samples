package com.r3.developers.samples.fx.services

import net.corda.v5.base.exceptions.CordaRuntimeException
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class FxService() {

    private companion object{
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val INVALID_CURRENCY_PAIR = "Invalid currencyPair given"
    }

    fun quoteFxRate(currencyPair: String): BigDecimal {
        "$INVALID_CURRENCY_PAIR: '$currencyPair'" using { enumContains<ConversionRateTable>(currencyPair) }
        val conversionRate = ConversionRateTable.valueOf(currencyPair).conversionRate
        log.info("currencyPair:$currencyPair | conversionRate:$conversionRate")
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
//convention for currencyPairing: "[currency_from][currency_to]"
enum class ConversionRateTable(val conversionRate: BigDecimal) {
    //TODO put a date/source.
    GBPGBP(BigDecimal(1)),
    GBPEUR(BigDecimal(1.16)),
    GBPUSD(BigDecimal(1.27)),
    GBPCAD(BigDecimal(1.72)),
    EURGBP(BigDecimal(0.85)),
    EUREUR(BigDecimal(1)),
    EURUSD(BigDecimal(1.08)),
    EURCAD(BigDecimal(1.47)),
    USDGBP(BigDecimal(0.78)),
    USDEUR(BigDecimal(0.91)),
    USDUSD(BigDecimal(1)),
    USDCAD(BigDecimal(1.35)),
    CADGBP(BigDecimal(0.58)),
    CADEUR(BigDecimal(0.67)),
    CADUSD(BigDecimal(0.73)),
    CADCAD(BigDecimal(1))

}