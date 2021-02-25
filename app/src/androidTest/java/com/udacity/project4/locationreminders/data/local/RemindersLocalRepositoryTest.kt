package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
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

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun initDbAndRepository() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), TestCoroutineDispatcher())
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndFindById() = runBlockingTest {
        val reminder = makeReminder("Reminder 1", "A description", "Somewhere")
        repository.saveReminder(reminder)

        val result = repository.getReminder(reminder.id)

        assertThat(result is Result.Success, `is`(true))

        if (result is Result.Success) {
            val loaded = result.data
            assertThat(loaded as ReminderDTO, CoreMatchers.notNullValue())
            assertThat(loaded.id, `is`(reminder.id))
            assertThat(loaded.title, `is`(reminder.title))
            assertThat(loaded.description, `is`(reminder.description))
            assertThat(loaded.longitude, `is`(reminder.longitude))
            assertThat(loaded.latitude, `is`(reminder.latitude))
            assertThat(loaded.location, `is`(reminder.location))
        }
    }

    @Test
    fun getByIncorrectIdReturnsResultError() = runBlockingTest {
        val id = "123456789101121231415"

        val result = repository.getReminder(id)

        assertThat(result is Result.Error, `is`(true))
    }

    @Test
    fun getAllRemindersNotEmpty() = runBlockingTest {
        val reminder = makeReminder("Reminder A", "Reminder A Description", "Somewhere")
        val reminderTwo = makeReminder("Reminder B", "Reminder B Description", "Somewhere")
        repository.saveReminder(reminder)
        repository.saveReminder(reminderTwo)

        val result: Result<List<ReminderDTO>> = repository.getReminders()

        assertThat(result is Result.Success, `is`(true))

        if (result is Result.Success) {
            assertThat(result.data.isNotEmpty(), `is`(true))
        }
    }

    @Test
    fun addAndDeleteSingleReminder() = runBlockingTest {
        val reminder = makeReminder("To Be Delted", "....", "Somewhere")
        repository.saveReminder(reminder)
        repository.deleteReminder(reminder)

        val result = repository.getReminder(reminder.id)

        assertThat(result is Result.Error, `is`(true))
        if (result is Result.Error) {
            assertThat(result.message, `is`("Reminder not found!"))
        }
    }

    @Test
    fun deleteAllRemindersListEmpty() = runBlockingTest {
        val reminder = makeReminder("To Be Delted", "....", "Somewhere")
        repository.saveReminder(reminder)
        repository.deleteAllReminders()

        val result = repository.getReminders()

        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) {
            val list = result.data

            assertThat(list.isEmpty(), `is`(true))
        }
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