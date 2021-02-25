package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
                getApplicationContext(),
                RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertManyRemindersAndFetchAll() = runBlocking {
        val reminder = makeReminder("1", "", "")
        val reminderTwo = makeReminder("1", "", "")
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminderTwo)

        val loaded = database.reminderDao().getReminders()

        assertThat(loaded.isNotEmpty(), `is`(true))
    }

    @Test
    fun insertReminderAndFindById() = runBlockingTest {
        // Given that a task is inserted
        val reminder = makeReminder("Reminder Insert Test", "An Insert Test", "Custom Location")
        database.reminderDao().saveReminder(reminder)

        // Load task
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // Ensure that a reminder has been fetched and that it is the correct one.
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.longitude, `is`(reminder.longitude))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.location, `is`(reminder.location))
    }

    @Test
    fun insertReminderOnConflictReplace() = runBlockingTest {
        // Given that one reminder is created and another reminder is saved with the same id
        val reminder = makeReminder("Reminder 1", "The first reminder", "Location 1")
        database.reminderDao().saveReminder(reminder)

        val newReminder = ReminderDTO(
                "Reminder 2",
                "Another reminder",
                "Another location",
                0.00,
                0.00,
                id = reminder.id
        )
        database.reminderDao().saveReminder(newReminder)

        // Load a reminder with the original id
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // Check the loaded reminder is the new reminder (that the first once has been replaced due to OnConflictStrategy.Replace)
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(newReminder.id))
        assertThat(loaded.title, `is`(newReminder.title))
        assertThat(loaded.description, `is`(newReminder.description))
        assertThat(loaded.longitude, `is`(newReminder.longitude))
        assertThat(loaded.latitude, `is`(newReminder.latitude))
        assertThat(loaded.location, `is`(newReminder.location))
    }

    @Test
    fun insertReminderAndDelete() = runBlockingTest {
        // Given a reminder with a particular id, insert it into the database and then delete it
        val reminder = makeReminder("To Be Deleted", "", "")
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteReminder(reminder)

        // Try to load a reminder with the original id
        var loaded: ReminderDTO? = database.reminderDao().getReminderById(reminder.id)

        // Check that the loaded value is null (there is no result
        assertThat(loaded, nullValue())
    }

    @Test
    fun insertReminderAndDeleteAll() = runBlockingTest {
        // Given at least one reminder in the database, delete all
        val reminder = makeReminder("To Be Deleted", "", "")
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()

        val loaded = database.reminderDao().getReminders()

        assertThat(loaded.isEmpty(), `is`(true))
    }

    private fun makeReminder(title: String, description: String, location: String): ReminderDTO {
        val reminder = ReminderDTO(
                title = title,
                description = description,
                longitude = 123.00,
                latitude = 123.00,
                location = location
        )
        return reminder
    }
}