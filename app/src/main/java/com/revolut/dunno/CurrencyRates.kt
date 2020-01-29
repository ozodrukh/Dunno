package com.revolut.dunno

data class CurrencyRates(
  val base: String,
  val date: String,
  val rates: Map<String, Double>
) {

  operator fun get(currency: String): Double {
    return rates[currency] ?: 0.0
  }

  fun ratio(base: String, desired: String): Double {
    if (base == desired) {
      return 1.0
    }

    if (this.base == desired) {
      return 1 / this[base]
    } else if (this.base == base) {
      return this[desired]
    } else {
      return this[desired] / this[base]
    }
  }

  companion object {
    val currencyEmojiMap = mapOf(
        "AUD" to "🇦🇺",
        "BGN" to "🇧🇬",
        "BRL" to "🇧🇷",
        "CAD" to "🇨🇦",
        "CHF" to "🇨🇭",
        "CNY" to "🇨🇳",
        "CZK" to "🇨🇿",
        "DKK" to "🇩🇰",
        "GBP" to "🏴󠁧󠁢󠁥󠁮󠁧󠁿󠁧󠁢󠁥󠁮󠁧󠁿",
        "HKD" to "🇭🇰",
        "HRK" to "🇭🇷",
        "HUF" to "🇭🇺",
        "IDR" to "🇮🇩",
        "ILS" to "🇮🇱",
        "INR" to "🇧🇹",
        "ISK" to "🇮🇸",
        "JPY" to "🇯🇵",
        "KRW" to "🇰🇷",
        "MXN" to "🇲🇽",
        "MYR" to "🇲🇾",
        "NOK" to "🇳🇴",
        "NZD" to "🇳🇿",
        "PHP" to "🇵🇭",
        "PLN" to "🇵🇱",
        "RON" to "🇷🇴",
        "RUB" to "🇷🇺",
        "SEK" to "🇸🇪",
        "SGD" to "🇸🇬",
        "THB" to "🇹🇭",
        "TRY" to "🇹🇷",
        "USD" to "🇺🇸",
        "ZAR" to "🇿🇲",
        "EUR" to "🇪🇺"
    )
  }
}