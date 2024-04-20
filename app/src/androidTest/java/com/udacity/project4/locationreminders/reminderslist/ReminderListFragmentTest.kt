package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest(){

    //variables
    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var application: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    //initialize koin
    @Before
    fun init() {
        //stop main koin
        stopKoin()

        application = getApplicationContext()

        // register the module classes
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(application) }
        }

        //initialize koin for the tests
        startKoin {
            modules(listOf(myModule))
        }

        //getting the dependencies
        reminderDataSource = get()

        //clear database data
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }

    //register idling resource
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }


    // Unregister idling Resource
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun testFragmentsNavigation() {

        //create container so the idling can monitor it
        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        //navigation object to monitor the screen
        val navController = Mockito.mock(NavController::class.java)

        //setting the navController
        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        //navigate...
        //then verify the navigation direction
        Espresso.onView(withId(R.id.addReminderFAB)).perform(click())
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun testNoDataDisplayed() {
        //contain the fragment and idling it to monitor
        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        // find the view that displays "No Data" message to verify it
        Espresso.onView(ViewMatchers.withText(R.string.no_data))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }

}