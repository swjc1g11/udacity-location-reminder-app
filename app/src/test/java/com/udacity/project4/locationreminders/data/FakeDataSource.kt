package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    companion object {
        const val CONTRIVED_ERROR_MESSAGE = "An error!"
    }

    private var remindersList = mutableListOf<ReminderDTO>()
    var throwErrors = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (throwErrors) return Result.Error(CONTRIVED_ERROR_MESSAGE)
        return Result.Success<List<ReminderDTO>>(remindersList)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (throwErrors) return Result.Error(CONTRIVED_ERROR_MESSAGE)

        var result: ReminderDTO? = null
        for (reminder in remindersList) {
            if (reminder.id == id) {
                result = reminder
                break;
            }
        }

        if (result != null) {
            return Result.Success<ReminderDTO>(result)
        }

        return Result.Error("No reminder with the id $id could be found.", 404)
    }

    override suspend fun deleteAllReminders() {
        remindersList = mutableListOf<ReminderDTO>()
    }

    override suspend fun deleteReminder(reminder: ReminderDTO) {
        remindersList.remove(reminder)
    }
}