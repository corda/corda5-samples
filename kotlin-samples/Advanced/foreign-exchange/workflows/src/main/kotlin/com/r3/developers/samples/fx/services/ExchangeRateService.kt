package com.r3.developers.samples.fx.services

import com.r3.developers.samples.fx.contract.CurrencyCode
import java.math.BigDecimal

object ExchangeRateService {

    private val exchangeRates = mapOf(

        // Conversions from GBP
        (CurrencyCode.GBP to CurrencyCode.USD) to 1.27.toBigDecimal(),
        (CurrencyCode.GBP to CurrencyCode.EUR) to 1.16.toBigDecimal(),
        (CurrencyCode.GBP to CurrencyCode.CAD) to 1.72.toBigDecimal(),

        // Conversions from USD
        (CurrencyCode.USD to CurrencyCode.GBP) to 0.78.toBigDecimal(),
        (CurrencyCode.USD to CurrencyCode.EUR) to 0.91.toBigDecimal(),
        (CurrencyCode.USD to CurrencyCode.CAD) to 1.35.toBigDecimal(),

        // Conversions from EUR
        (CurrencyCode.EUR to CurrencyCode.GBP) to 0.85.toBigDecimal(),
        (CurrencyCode.EUR to CurrencyCode.USD) to 1.08.toBigDecimal(),
        (CurrencyCode.EUR to CurrencyCode.CAD) to 1.47.toBigDecimal(),

        // Conversions from CAD
        (CurrencyCode.CAD to CurrencyCode.GBP) to 0.58.toBigDecimal(),
        (CurrencyCode.CAD to CurrencyCode.USD) to 0.73.toBigDecimal(),
        (CurrencyCode.CAD to CurrencyCode.EUR) to 0.67.toBigDecimal()
    )

    fun getExchangeRate(convertingFrom: CurrencyCode, convertingTo: CurrencyCode): BigDecimal {
        require(exchangeRates.containsKey(convertingFrom to convertingTo)) {
            "Cannot convert from $convertingFrom to $convertingTo"
        }

        return exchangeRates.getValue(convertingFrom to convertingTo)
    }
}