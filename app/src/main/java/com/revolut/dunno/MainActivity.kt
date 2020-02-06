package com.revolut.dunno

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.Toolbar
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.revolut.dunno.CurrencyAdapter.CurrencyViewHolder
import java.util.*

class MainActivity : AppCompatActivity() {
  private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  private val container by lazy { findViewById<RecyclerView>(R.id.container) }
  private val circularProgress by lazy { findViewById<ProgressBar>(R.id.progress_circular) }

  private var skipSelfInteraction = false

  @VisibleForTesting internal var factory: ViewModelProvider.NewInstanceFactory? = null

  private val liveRatesModel: LiveRatesViewModel by lazy {
    ViewModelProvider(viewModelStore, factory ?: AndroidViewModelFactory(application)).get(
        LiveRatesViewModel::class.java
    )
  }

  private val userInteractWithInput = { value: Double ->
    if (!skipSelfInteraction) {
      refreshRatesOnVisibleRows(liveRatesModel.rates)
    } else {
      skipSelfInteraction = false
    }
  }

  private val onActiveRowChanged = { target: ViewHolder ->
    liveRatesModel.currency = adapter.currencies[target.adapterPosition]

    adapter.moveRowToTop(target.adapterPosition)

    if (target is CurrencyViewHolder) {
      adapter.primaryCurrencyValue = adapter.getAmount(target.rate.text)

      refreshRatesOnVisibleRows(liveRatesModel.rates)
    }
  }

  private var defaultCurrency = Currency.getInstance(Locale.US).currencyCode

  private val adapter = CurrencyAdapter(defaultCurrency)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // was a bit lazy to make Application class for emoji initializing
    EmojiCompat.init(BundledEmojiCompatConfig(this))

    setContentView(R.layout.activity_main)

    container.adapter = adapter

    setupLiveRates()
  }

  fun setupLiveRates() {
    liveRatesModel.currency = defaultCurrency
    liveRatesModel.listen(this, Observer { rates ->
      adapter.currencyRates = rates

      if (adapter.currencies.isEmpty()) {
        adapter.addCurrencies(rates)

        circularProgress.visibility = View.GONE
      }

      refreshRatesOnVisibleRows(rates)

      toolbar.subtitle = if (!rates.offline) null else {
        getString(R.string.offline_rates)
      }
    })
  }

  private fun refreshRatesOnVisibleRows(rates: CurrencyRates) {
    container.visibleRows<CurrencyViewHolder> { vh ->
      // in order not to loose focus & state on active view, we skip handling it
      if (adapter.currencies[vh.adapterPosition] == rates.base) {
        return@visibleRows
      }

      skipSelfInteraction = true

      vh.setCurrencyValue(
          adapter.primaryCurrencyValue * rates.ratio(
              liveRatesModel.currency, adapter.currencies[vh.adapterPosition]
          )
      )
    }
  }

  override fun onStart() {
    super.onStart()
    adapter.onUserEditInput = userInteractWithInput
    adapter.onFocusChange = onActiveRowChanged
  }

  override fun onStop() {
    super.onStop()
    adapter.onUserEditInput = null
    adapter.onFocusChange = null
  }
}
