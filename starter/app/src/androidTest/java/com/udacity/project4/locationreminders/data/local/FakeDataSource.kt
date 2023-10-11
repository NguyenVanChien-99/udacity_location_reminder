package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnError){
            return Result.Error("Get reminder error")
        }
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Get reminder error")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnError){
            return Result.Error("Get reminder error")
        }
        val reminder = reminders?.firstOrNull{it.id==id} ?: return Result.Error("Not found")
        return Result.Success(reminder)
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}