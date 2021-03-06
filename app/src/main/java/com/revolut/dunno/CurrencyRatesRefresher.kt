package com.revolut.dunno

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class CurrencyRatesRefresher(retrofit: Retrofit) {
  companion object {
    val EMPTY_CURRENCY_RATE = CurrencyRates(
        Currency.getInstance(Locale.US).currencyCode, Date().toString(), emptyMap()
    )
  }

  private val scheduler = ScheduledThreadPoolExecutor(1)
  private var scheduledTask: ScheduledFuture<*>? = null

  private val ratesService by lazy {
    retrofit.create<CurrencyRatesService>()
  }

  private val mutableLiveRates = MutableLiveData<CurrencyRates>()
  private val safeCurrencyCode = AtomicReference<String>(EMPTY_CURRENCY_RATE.base)

  val currentCurrency: String
    get() = safeCurrencyCode.get()

  val currentRates: CurrencyRates
    get() = mutableLiveRates.value ?: EMPTY_CURRENCY_RATE

  val liveRates: LiveData<CurrencyRates> = mutableLiveRates

  init {
    scheduler.removeOnCancelPolicy = true
  }

  private fun dispatchUpdateRates() {
    try {
      val targetCurrency = currentCurrency
      val response = ratesService.get(targetCurrency).execute()

      if (response.isSuccessful) {
        val body = response.body()

        if (body != null) {
          mutableLiveRates.postValue(body)
        }

        // todo: should we notify anyone about parse error?
      }
      // todo: should we handle exceptions if any?
    } catch (e: Exception) {
      e.printStackTrace()

      if (e is IOException) { // due to lack of network
        mutableLiveRates.postValue(currentRates.copy(date = "offline"))
      }
    }
  }

  fun setCurrency(value: String) {
    safeCurrencyCode.getAndSet(value)

    // trigger immediate currencyName update if user changed currencyName
    scheduler.execute(this::dispatchUpdateRates)
  }

  fun running(): Boolean {
    return !(scheduledTask?.isCancelled ?: true)
  }

  @MainThread
  fun start() {
    stop() // remove existing task

    scheduledTask = scheduler.scheduleAtFixedRate(
        this::dispatchUpdateRates, 0, 1, TimeUnit.SECONDS
    )
  }

  @MainThread
  fun stop() {
    scheduledTask?.cancel(false)
  }

  interface CurrencyRatesService {
    @GET("/latest")
    fun get(@Query("base") targetCurrency: String): Call<CurrencyRates>
  }
}