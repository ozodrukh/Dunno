package com.revolut.dunno

import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.emoji.widget.EmojiTextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.revolut.dunno.CurrencyAdapter.CurrencyViewHolder

class CurrencyAdapter(private var primaryCurrency: String) : Adapter<CurrencyViewHolder>() {
  var currencyRates: CurrencyRates = CurrencyRatesRefresher.EMPTY_CURRENCY_RATE
  var currencies: List<String> = emptyList()
  var primaryCurrencyValue = 100.0

  var onUserEditInput: ((Double) -> Unit)? = null
  var onFocusChange: ((ViewHolder) -> Unit)? = null

  private val filters = arrayOf(object : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int,
      dest: Spanned, dstart: Int, dend: Int): CharSequence {

      if (source == "0" && dest.length == 1) {
        dest.toString().toDoubleOrNull()?.let { value ->
          return if (value == 0.0) "" else source
        }
      }

      return source
    }
  })

  private val inputObserver = object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      if (s != null) {
        primaryCurrencyValue = getAmount(s)
        onUserEditInput?.invoke(primaryCurrencyValue)
      }
    }
  }

  fun moveRowToTop(fromPosition: Int) {
    (currencies as MutableList).apply {
      add(0, removeAt(fromPosition))

      notifyItemMoved(fromPosition, 0)
    }
  }

  fun getAmount(value: CharSequence): Double {
    return value.toString().toDoubleOrNull() ?: 0.0
  }

  fun addCurrencies(updated: CurrencyRates) {
    if (updated.rates.isEmpty()) {
      return // skip stub item
    }

    val currencies = ArrayList<String>()
    currencies += updated.base
    currencies += updated.rates.keys

    this.currencies = currencies

    notifyItemRangeInserted(0, currencies.size)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
    return CurrencyViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_rate_view, parent, false)
    ).apply {

      rate.filters = filters
    }
  }

  override fun getItemCount(): Int {
    return currencies.size
  }

  override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
    holder.setCurrency(currencies[position])
    holder.setCurrencyValue(
        primaryCurrencyValue * currencyRates.ratio(
            primaryCurrency, currencies[position]
        )
    )
  }

  override fun onViewAttachedToWindow(holder: CurrencyViewHolder) {
    super.onViewAttachedToWindow(holder)

    holder.rate.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        primaryCurrency = currencies[holder.adapterPosition]
        holder.rate.addTextChangedListener(inputObserver)

        onFocusChange?.invoke(holder)
      } else {
        holder.rate.removeTextChangedListener(inputObserver)
      }
    }
  }

  override fun onViewDetachedFromWindow(holder: CurrencyViewHolder) {
    super.onViewDetachedFromWindow(holder)

    holder.rate.setOnFocusChangeListener(null)
    holder.rate.removeTextChangedListener(inputObserver)
  }

  class CurrencyViewHolder(itemView: View) : ViewHolder(itemView) {
    private val title = itemView.findViewById<TextView>(R.id.title)
    private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
    private val countryFlag = itemView.findViewById<EmojiTextView>(R.id.country_flag)

    internal val rate = itemView.findViewById<EditText>(R.id.rate)

    val currencyName: String
      get() = title.text.toString()

    fun setCurrency(currencyCode: String) {
      title.text = currencyCode
      subtitle.text = currencyCode.currencyName()
      countryFlag.text = CurrencyRates.currencyEmojiMap[currencyCode]
    }

    fun setCurrencyValue(value: Double) {
      if (Math.floor(value) == value) {
        rate.setText(String.format("%d", value.toInt()))
      } else {
        rate.setText(String.format("%.2f", value))
      }
    }
  }
}