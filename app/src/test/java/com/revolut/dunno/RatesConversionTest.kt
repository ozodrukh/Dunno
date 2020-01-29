package com.revolut.dunno

import org.junit.Assert
import org.junit.Test

class RatesConversionTest {

  @Test
  fun `check conversion`() {
    val rates = CurrencyRates(
        "EUR", "", mapOf(
        "USD" to 1.1615
    )
    )

    Assert.assertEquals(rates.ratio("EUR", "USD") * 100, 116.15, 0.1)
  }

  @Test
  fun `check local conversion`() {
    val rates = CurrencyRates(
        "EUR", "", mapOf(
        "USD" to 1.1615
    )
    )

    Assert.assertEquals(rates.ratio("USD", "EUR") * 100, 86.09, 0.1)
  }

  @Test
  fun `check local conversion 2`() {
    val rates = CurrencyRates(
        "EUR", "", mapOf(
        "USD" to 1.1615,
        "THB" to 38.067
    )
    )

    val actual = rates.ratio("USD", "THB") * 100

    val usd100 = rates.ratio("USD", "EUR") * 100
    Assert.assertEquals(rates.ratio("EUR", "THB") * usd100, actual, 0.1)
  }
}
