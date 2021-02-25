package com.udacity.project4.locationreminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class ReminderDescriptionViewModel(val app: Application, val dataSource: ReminderDataSource) : AndroidViewModel(app) {

    val reminderHasBeenDeleted = MutableLiveData<Boolean>(false)

    fun deleteReminder(reminderItem: ReminderDataItem) {
        viewModelScope.launch {
            dataSource.deleteReminder(ReminderDTO(
                    id = reminderItem.id,
                    title = reminderItem.title,
                    description = reminderItem.description,
                    location = reminderItem.location,
                    longitude = reminderItem.longitude,
                    latitude = reminderItem.latitude
            ))
            reminderHasBeenDeleted.value = true
        }
    }
}