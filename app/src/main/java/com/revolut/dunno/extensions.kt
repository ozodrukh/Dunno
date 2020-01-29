package com.revolut.dunno

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import retrofit2.Retrofit
import java.util.Currency

private val cachedCurrencyMap = hashMapOf<String, Currency>()

fun String.currencyName(): String? {
  var currency = cachedCurrencyMap[this]
  if (currency == null) {
    currency = Currency.getInstance(this)
    cachedCurrencyMap[this] = currency
  }

  return currency?.displayName
}

inline fun <reified T> Retrofit.create(): T {
  return create(T::class.java)
}

fun <T : ViewHolder> RecyclerView.visibleRows(block: (T) -> Unit) {
  for (i in 0 until childCount) {
    (findViewHolderForLayoutPosition(i) as T?)?.let(block)
  }
}