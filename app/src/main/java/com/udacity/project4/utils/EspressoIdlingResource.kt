package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    //variables
    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    //call when starting asynchronous task
    fun increment() {
        countingIdlingResource.increment()
    }

    // call when asynchronous task completed
    fun decrement() {
        /* protect [countingIdlingResource] from being negative */
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}
    //wrap the asynchronous tasks
    inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {

        EspressoIdlingResource.increment()

    return try {

            function()

    } finally {

            EspressoIdlingResource.decrement()

    }

}