package com.revolut.dunno

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.revolut.dunno.CurrencyRatesRefresher.Companion.EMPTY_CURRENCY_RATE
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CurrencyRatesRefresherTest {
  companion object {
    private const val FAKE_JSON =
      "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1.6137,\"BGN\":1.9526,\"BRL\":4.7839,\"CAD\":1.5313,\"CHF\":1.1256,\"CNY\":7.9321,\"CZK\":25.673,\"DKK\":7.4445,\"GBP\":0.89677,\"HKD\":9.1174,\"HRK\":7.4219,\"HUF\":325.95,\"IDR\":17295.0,\"ILS\":4.1638,\"INR\":83.58,\"ISK\":127.59,\"JPY\":129.34,\"KRW\":1302.6,\"MXN\":22.329,\"MYR\":4.8041,\"NOK\":9.76,\"NZD\":1.7604,\"PHP\":62.489,\"PLN\":4.3112,\"RON\":4.6309,\"RUB\":79.444,\"SEK\":10.573,\"SGD\":1.5974,\"THB\":38.067,\"TRY\":7.6157,\"USD\":1.1615,\"ZAR\":17.794}}"
  }

  @Rule
  @JvmField
  val instantExecutorRule = InstantTaskExecutorRule()

  val currencyRefresher = CurrencyRatesRefresher(NetworkModule.retrofit)

  @Before
  fun init() {
    currencyRefresher.currentCurrency = "EUR"
  }

  @Test
  fun `test refresher executes repeated task`() {
    val times = 5

    val server = MockWebServer()
    for (i in 0 until times) {
      server.enqueue(MockResponse().setBody(FAKE_JSON))
    }
    server.start()

    val latch = CountDownLatch(times)

    val observer = Observer<CurrencyRates> {
      latch.countDown()

      assertNotEquals(currencyRefresher.currentRates, CurrencyRatesRefresher.EMPTY_CURRENCY_RATE)
    }

    currencyRefresher.liveRates.observeForever(observer)
    currencyRefresher.start()

    try {
      assertTrue(latch.await(7, SECONDS))
    } finally {
      currencyRefresher.liveRates.removeObserver(observer)
      server.shutdown()
    }
  }

  @Test
  fun `test refresh stops repeated task`() {
    val times = 5

    val server = MockWebServer()
    for (i in 0 until times) {
      server.enqueue(MockResponse().setBody(FAKE_JSON))
    }
    server.start()

    val latch = CountDownLatch(times)

    val observer = Observer<CurrencyRates> {
      latch.countDown()

      if (latch.count == 2L) {
        currencyRefresher.stop()
      }
    }

    currencyRefresher.liveRates.observeForever(observer)
    currencyRefresher.start()

    try {
      assertFalse(latch.await(7, SECONDS))
    } finally {

      currencyRefresher.liveRates.removeObserver(observer)
      server.shutdown()
    }
  }

  @Test
  fun `test changing currency triggers update`() {
    val times = 2

    val server = MockWebServer()
    server.enqueue(MockResponse().setBody(FAKE_JSON))
    server.start()

    val latch = CountDownLatch(times)

    val observer = Observer<CurrencyRates> {
      assertNotEquals(currencyRefresher.currentRates, EMPTY_CURRENCY_RATE)

      latch.countDown()
    }

    currencyRefresher.liveRates.observeForever(observer)
    currencyRefresher.currentCurrency = "USD"

    try {
      assertTrue(latch.await(5, SECONDS))
      assertEquals(currencyRefresher.currentCurrency, "USD")
    } finally {
      currencyRefresher.liveRates.removeObserver(observer)
      server.shutdown()
    }
  }
}
