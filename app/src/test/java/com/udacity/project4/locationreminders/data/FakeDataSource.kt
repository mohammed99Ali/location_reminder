package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private val dao: MutableList<ReminderDTO> = mutableListOf<ReminderDTO>(),
    private var isCorrect: Boolean = false
) :
    ReminderDataSource {

    //set the isCorrect value
    fun setIsCorrect(value: Boolean) {
        isCorrect = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return try {
            //check if the list has reminders
            //if empty throw Exception
            if (isCorrect) {
                throw Exception("Unable to retrieve reminders")
            }

            //return list of dao
            Result.Success(ArrayList(dao))

        } catch (ex: Exception) {
            //if error happen return error + Exception
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //save reminder to the database
        dao.add(reminder)
    }

    /*TODO:fakeData*/
    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return try {
            //search the database for the reminder
            val reminder = dao.find { it.id == id }

            //check if reminder exist then pass the reminder
            if (reminder == null)
                Result.Error("Unable to retrieve reminders")
            else if (isCorrect)
                Result.Error("Reminder with ID $id does not exist!")
            else
                Result.Success(reminder)

        } catch (ex: Exception) {
            //if error happen return error + Exception
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
    dao.clear()
    }
}