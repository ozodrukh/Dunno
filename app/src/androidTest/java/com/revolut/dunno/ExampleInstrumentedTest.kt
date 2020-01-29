package com.revolut.dunno

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.revolut.dunno.CurrencyAdapter.CurrencyViewHolder
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

  @get:Rule
  val rule = ActivityTestRule<MainActivity>(MainActivity::class.java, false, true)

  private val rates = CurrencyRates(
      "USD", "", mapOf("EUR" to 0.83)
  )
  private var currentCurrency: String = rates.base
  private val mockLiveData = MutableLiveData<CurrencyRates>()
  private val refresher: CurrencyRatesRefresher = mock {
    on { liveRates }.thenReturn(mockLiveData)
    on { currentRates }.thenReturn(rates)
    on { currentCurrency }.thenReturn(currentCurrency)

    doAnswer {
      currentCurrency = it.arguments[0] as String
    }.`when`(mock)
        .setCurrency(anyString())
  }

  @Before
  fun init() {
    mockLiveData.postValue(rates)

    rule.runOnUiThread {
      (rule.activity as MainActivity).setupCurrencyRefresher(refresher)
    }
  }

  @Test
  fun test() {
    onView(withId(R.id.container))
        .perform(actionOnHolderItem(FindRowWithTitle(Matchers.equalTo("EUR")), object : ViewAction {
          override fun getDescription(): String = "EditText typing"

          override fun getConstraints(): Matcher<View> =
            allOf(isAssignableFrom(EditText::class.java), isDisplayed())

          override fun perform(uiController: UiController?, view: View) {
            val t = (view as ViewGroup).getChildAt(3) as EditText
            t.requestFocus()
            t.setText("100")
          }
        }))
        .check(matches(allOf(object : TypeSafeMatcher<View>() {
          val holderMatcher = FindRowWithTitle(Matchers.equalTo("EUR"))

          override fun describeTo(description: Description) {
            description.appendText("Expected to be first")
          }

          override fun matchesSafely(item: View): Boolean {
            item as RecyclerView

            for (x in 0 until item.childCount) {
              val child = item.findViewHolderForLayoutPosition(x) ?: continue

              if (holderMatcher.matches(child) && child.adapterPosition == 0) {
                return true
              }
            }

            return false;
          }
        })))
  }

  fun nthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

      override fun describeTo(description: Description) {
        description.appendText("with $childPosition child view of type parentMatcher");
      }

      override fun matchesSafely(view: View): Boolean {
        if (!(view.getParent() is ViewGroup)) {
          return parentMatcher.matches(view.getParent());
        }

        val group = view.getParent() as ViewGroup;
        return parentMatcher.matches(view.getParent()) && group.getChildAt(childPosition).equals(
            view
        );
      }
    };
  }

  class FindRowWithTitle(val text: Matcher<String>) : TypeSafeMatcher<RecyclerView.ViewHolder>() {
    override fun describeTo(description: Description) {
      description.appendText("Couldn't find")
    }

    override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
      if(item is CurrencyViewHolder) {
        return text.matches(item.currencyName)
      } else {
        return false
      }
    }
  }
}
