package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.MyApp
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.FakeFirebaseUserLiveData
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsInstanceOf
import org.hamcrest.core.IsNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(maxSdk = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


    // Changes the main dispatcher so that it does not depend on the Android Looper
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    // Use with LiveData to ensure architechture background jobs run on the same thread
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: ReminderDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    val modulesToLoad = module {
        single(override = true) { FakeFirebaseUserLiveData() as LiveData<FirebaseUser?>  }
    }

    @Before
    fun setUpViewModel() = runBlockingTest {
        remindersRepository = FakeDataSource()

        val app: MyApp = ApplicationProvider.getApplicationContext()
        loadKoinModules(modulesToLoad)

        saveReminderViewModel = SaveReminderViewModel(app, remindersRepository)
    }

    @After
    fun tearDownViewModel() = runBlockingTest {
        unloadKoinModules(modulesToLoad)
        stopKoin()
    }

    @Test
    fun confirmLocationAndNavigateBack() = runBlockingTest {
        val latLng = LatLng(120.00, 120.00)
        val pointOfInterest = PointOfInterest(latLng, "Place One", "A Special Place")

        saveReminderViewModel.confirmLocation(latLng, pointOfInterest)

        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(latLng.latitude))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(latLng.longitude))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(pointOfInterest))
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(pointOfInterest.name))
        assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(), IsInstanceOf(NavigationCommand.Back::class.java))

        saveReminderViewModel.onClear()
    }

    @Test
    fun validateCorrectAndIncorrectReminder() = runBlockingTest {
        val badReminder = ReminderDataItem(
            title = "Bad Reminder",
            description = null,
            longitude = null,
            latitude = null,
            location = null)
        val badReminderTwo = ReminderDataItem (
            title = null,
            description = null,
            longitude = null,
            latitude = null,
            location = "A Location"
        )
        val goodReminder = ReminderDataItem(
            title = "Good Reminder",
            description = null,
            longitude = null,
            latitude = null,
            location = "Good Reminder Location"
        )

        val badReminderResult = saveReminderViewModel.validateEnteredData(badReminder)
        assertThat(badReminderResult, `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), notNullValue())
        saveReminderViewModel.showSnackBarInt.value = null

        val goodReminderResult = saveReminderViewModel.validateEnteredData(goodReminder)
        assertThat(goodReminderResult, `is`(true))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), IsNull())
        saveReminderViewModel.showSnackBarInt.value = null

        val badReminderTwoResult = saveReminderViewModel.validateEnteredData(badReminderTwo)
        assertThat(badReminderTwoResult, `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), notNullValue())
        saveReminderViewModel.showSnackBarInt.value = null
    }

    @Test
    fun validateAndSaveReminders() = runBlockingTest {
        val badReminder = ReminderDataItem(
            title = "Bad Reminder",
            description = null,
            longitude = null,
            latitude = null,
            location = null)
        val badReminderTwo = ReminderDataItem (
            title = null,
            description = null,
            longitude = null,
            latitude = null,
            location = "A Location"
        )
        val goodReminder = ReminderDataItem(
            title = "Good Reminder",
            description = null,
            longitude = null,
            latitude = null,
            location = "Good Reminder Location"
        )

        saveReminderViewModel.validateAndSaveReminder(goodReminder)

        saveReminderViewModel.validateAndSaveReminder(badReminder)
        val badReminderResult = remindersRepository.getReminder(badReminder.id)
        assertThat(badReminderResult, IsInstanceOf(Result.Error::class.java))

        saveReminderViewModel.validateAndSaveReminder(badReminderTwo)
        val badReminderTwoResult = remindersRepository.getReminder(badReminderTwo.id)
        assertThat(badReminderTwoResult, IsInstanceOf(Result.Error::class.java))

        saveReminderViewModel.validateAndSaveReminder(badReminderTwo)
        val goodReminderResult = remindersRepository.getReminder(goodReminder.id)
        assertThat(goodReminderResult, IsInstanceOf(Result.Success::class.java))
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), notNullValue())
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(), IsInstanceOf(NavigationCommand.Back::class.java))


        if (goodReminderResult is Result.Success) {
            val data = goodReminderResult.data
            assertThat(data.id, `is`(goodReminder.id))
            assertThat(data.description, `is`(goodReminder.description))
            assertThat(data.location, `is`(goodReminder.location))
            assertThat(data.longitude, `is`(goodReminder.longitude))
            assertThat(data.latitude, `is`(goodReminder.latitude))
        }

        saveReminderViewModel.onClear()
    }
}