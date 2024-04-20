package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
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

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed auto close @after method to close Koin after every test

    //variables
    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var application: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        application = getApplicationContext()
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
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        reminderDataSource = get()

        //clear the data to start fresh
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }

    //register idling resource
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }
    //Unregister idling resource
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun testErrorEnterTitleSnackBar() {

        //launch the activity then monitor it with idling
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //add & save reminder with [.perform]
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        //display an error snack-bar with the message "Enter a title"
        val snackBarMessage = application.getString(R.string.err_enter_title)

        //find view to display the message...
        //verify that it displayed
        onView(withText(snackBarMessage)).check(matches(isDisplayed()))

        //close the activity
        activityScenario.close()
    }

    @Test
    fun testReminderSavedToastMessage() {
        //launch the activity then monitor it with idling
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //add reminder with [.perform]
        //replace the title and description
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Description"))
        //open map and save location
        onView(withId(R.id.selectLocation)).perform(click())
        //click on the map
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.save_button)).perform(click())
        //save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        //Wait for the first toast to disappear
        Thread.sleep(3500)

        //toast reminder saved
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(getActivity(activityScenario).window.decorView))))
            .check(matches(isDisplayed()))

        //close the Scenario
        activityScenario.close()
    }

    //getting activity for the Scenario
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}
