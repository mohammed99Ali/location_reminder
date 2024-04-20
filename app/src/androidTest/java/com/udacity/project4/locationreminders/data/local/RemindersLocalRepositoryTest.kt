package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    //initialize the database for each test
    @Before
    fun initializeDatabase() {
        /*using [inMemoryDatabaseBuilder] so the created data base
        exists only in the memory that based on the [RemindersDatabase]*/
            remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        remindersLocalRepository = RemindersLocalRepository(remindersDatabase.reminderDao())
    }

    //closes the database
    @After
    fun closeDatabase() = remindersDatabase.close()

    @Test
    fun testInsertRetrieveData() = runBlocking {

        //data for the test
        val testData = ReminderDTO(
            "Title",
            "Description",
            "Location",
            99.99,
            00.00)

        //save the test data to the database
        remindersLocalRepository.saveReminder(testData)

        //getting the reminder
        val testReminder = remindersLocalRepository.getReminder(testData.id)

        //set the reminder's result
        testReminder as Result.Success

        //test the reminder data statue
        MatcherAssert.assertThat(testReminder.data != null, CoreMatchers.`is`(true))

        //variable
        val reminderData = testReminder.data

        //test if the reminder's data is same as the test's data
        MatcherAssert.assertThat(reminderData.id, CoreMatchers.`is`(testData.id))
        MatcherAssert.assertThat(reminderData.title, CoreMatchers.`is`(testData.title))
        MatcherAssert.assertThat(reminderData.description, CoreMatchers.`is`(testData.description))
        MatcherAssert.assertThat(reminderData.location, CoreMatchers.`is`(testData.location))
        MatcherAssert.assertThat(reminderData.latitude, CoreMatchers.`is`(testData.latitude))
        MatcherAssert.assertThat(reminderData.longitude, CoreMatchers.`is`(testData.longitude))
    }

    @Test
    fun testDataNotFound_returnError() = runBlocking {
        //false call for reminder
        val testReminder = remindersLocalRepository.getReminder("123")
        //set the reminder result
        val reminderResultError =  testReminder is Result.Error

        //test if the [reminderResultError] result is error
        MatcherAssert.assertThat(reminderResultError, CoreMatchers.`is`(true))
    }
}