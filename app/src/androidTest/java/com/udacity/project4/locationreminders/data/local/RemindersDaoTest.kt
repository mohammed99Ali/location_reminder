package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    //variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var remindersDatabase: RemindersDatabase

    //initialize the database for each test
    @Before
    fun initializeDatabase() {
        /*using [inMemoryDatabaseBuilder] so the created data base
        exists only in the memory that based on the [RemindersDatabase]*/
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    //closes the database
    @After
    fun closeDatabase() = remindersDatabase.close()

    @Test
    fun testInsertRetrieveData() = runBlockingTest {

        // data for the test
        val testData = ReminderDTO(
            "Title",
            "Description",
            "Location",
            99.99,
            00.00
        )

        //save the test data to the database
        remindersDatabase.reminderDao().saveReminder(testData)

        //load the reminders to list
        val testReminders = remindersDatabase.reminderDao().getReminders()

        //test if the list has only one reminder
        MatcherAssert.assertThat(testReminders.size, `is`(1))

        //variable
        val reminder = testReminders[0]

        //test if the reminder data is the same as the test data
        MatcherAssert.assertThat(reminder.id, `is`(testData.id))
        MatcherAssert.assertThat(reminder.title, `is`(testData.title))
        MatcherAssert.assertThat(reminder.description, `is`(testData.description))
        MatcherAssert.assertThat(reminder.location, `is`(testData.location))
        MatcherAssert.assertThat(reminder.latitude, `is`(testData.latitude))
        MatcherAssert.assertThat(reminder.longitude, `is`(testData.longitude))

    }
}