package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


private lateinit var fakeDataSource: FakeDataSource
private lateinit var remindersViewModel: RemindersListViewModel

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    //inject rule and some variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //implement the view model
    @Before
    fun setupViewModel() {
        fakeDataSource = FakeDataSource()
        remindersViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    //set the fake data and save it
    private suspend fun saveFakeData() {
        fakeDataSource.saveReminder(
            ReminderDTO(
                "Data",
                "Description",
                "Location",
                99.99,
                00.00)
        )
    }

    @Test
    fun testShouldReturnError () = runBlocking   {
        //force to throw exception
        fakeDataSource.setIsCorrect(true)
        //save the data
        saveFakeData()
        //loading reminders
        remindersViewModel.loadReminders()

        //test if the reminder result is error based on the show snack bar value
        MatcherAssert.assertThat(
            remindersViewModel.showSnackBar.value, CoreMatchers.`is`("Reminders not found")
        )
    }

    @Test
    fun check_loading() = runBlocking  {

        //pause the dispatcher
        //save the fake data to the database
        //loading the reminder from the tabase
        mainCoroutineRule.pauseDispatcher()
        saveFakeData()
        remindersViewModel.loadReminders()

        //test if the reminders saved to the database based on the show loading value
        MatcherAssert.assertThat(remindersViewModel.showLoading.value, CoreMatchers.`is`(true))

        //test if the reminders saved to the database based on the show loading value
        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(remindersViewModel.showLoading.value, CoreMatchers.`is`(false))
    }


}

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher(), TestCoroutineScope by TestCoroutineScope(dispatcher) {

    // sets the main dispatcher to the TestCoroutineDispatcher
    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    //cleans up coroutines and resets the main dispatcher
    override fun finished(description: Description?) {
        super.finished(description)
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}