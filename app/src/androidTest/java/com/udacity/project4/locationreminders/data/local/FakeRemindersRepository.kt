package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeRemindersRepository : ReminderDataSource {

    companion object {
        const val CONTRIVED_ERROR_MESSAGE = "A contrived error message!"
    }

    private var reminders = mutableListOf<ReminderDTO>()
    var throwErrors = false


    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (throwErrors) return Result.Error(CONTRIVED_ERROR_MESSAGE)
        return Result.Success<List<ReminderDTO>>(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (throwErrors) return Result.Error(CONTRIVED_ERROR_MESSAGE)
        for (reminder in reminders) {
            if (reminder.id == id) return Result.Success<ReminderDTO>(reminder)
        }

        return Result.Error("Could not find a reminder with the id $id")
    }

    override suspend fun deleteReminder(reminder: ReminderDTO) {
        reminders.remove(reminder)
    }

    override suspend fun deleteAllReminders() {
        reminders = mutableListOf<ReminderDTO>()
    }
}