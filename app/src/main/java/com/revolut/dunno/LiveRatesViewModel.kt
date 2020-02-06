package com.revolut.dunno

import android.app.Application
import android.net.ConnectivityManager
import android.net.ConnectivityManager.OnNetworkActiveListener
import androidx.core.content.ContextCompat
import androidx.core.net.ConnectivityManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.lang.IllegalStateException

open class LiveRatesViewModel @JvmOverloads constructor(
  app: Application,
  val checkConnectivity: Boolean = BuildConfig.DEBUG) : AndroidViewModel(app) {

  private val refresher = CurrencyRatesRefresher(NetworkModule.retrofit)
  private val cm = ContextCompat.getSystemService(app, ConnectivityManager::class.java)
      ?: throw IllegalStateException("ConnectivityManager not found")

  private val networkCallback = OnNetworkActiveListener {
    if (!refresher.running()) {
      refresher.start()
    }
  }

  private val offlineChecker = Observer<CurrencyRates> {
    if (it.offline && refresher.running()) {
      refresher.stop()
    }
  }

  open var currency: String
    set(value) = refresher.setCurrency(value)
    get() = refresher.currentCurrency

  open val rates: CurrencyRates
    get() = refresher.liveRates.value ?: CurrencyRatesRefresher.EMPTY_CURRENCY_RATE

  init {
    if (checkConnectivity) {
      cm.addDefaultNetworkActiveListener(networkCallback)
      refresher.liveRates.observeForever(offlineChecker)
    }
  }

  open fun listen(parent: LifecycleOwner, observer: Observer<CurrencyRates>) {
    refresher.liveRates.observe(parent, observer)

    if (!checkConnectivity || cm.isDefaultNetworkActive) {
      refresher.start()
    }
  }

  override fun onCleared() {
    super.onCleared()

    if (checkConnectivity) {
      refresher.liveRates.removeObserver(offlineChecker)
      cm.removeDefaultNetworkActiveListener(networkCallback)
    }

    if (refresher.running()) {
      refresher.stop()
    }
  }
}