package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeRemindersRepository
import com.udacity.project4.util.FakeFirebaseUserLiveData
import com.udacity.project4.utils.FirebaseUserLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource

    private val moduleToLoad = module {
        single(override = true) { repository }
        single(override = true) { FakeFirebaseUserLiveData() as LiveData<FirebaseUser?>  }
    }

    @Before
    fun initRepository() = runBlockingTest {
        repository = FakeRemindersRepository()
        loadKoinModules(moduleToLoad)
    }

    @After
    fun cleanUpDB() = runBlockingTest {
        unloadKoinModules(moduleToLoad)
    }

    @Test
    fun checkItemsAreDisplayedWhenItemsPresent() = runBlockingTest {
        repository.deleteAllReminders()
        val reminderOne = makeReminder("Reminder One", "A description", "....");
        val reminderTwo = makeReminder("Reminder Two", "Another reminder", ".....")
        repository.saveReminder(reminderOne)
        repository.saveReminder(reminderTwo)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //scenario.onFragment {
            //val res = it._viewModel.authenticationState.getOrAwaitValue()
            //assertThat(res, `is`(BaseViewModel.AuthenticationState.AUTHENTICATED))
        //}

        onView(withId(R.id.reminderssRecyclerView)).check(RecyclerViewItemCountAssertion(2))
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun checkNoDataIsDispalyedWhenNoItemsPresent() = runBlockingTest {
        repository.deleteAllReminders()

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //scenario.onFragment {
            //val res = it._viewModel.authenticationState.getOrAwaitValue()
            //assertThat(res, `is`(BaseViewModel.AuthenticationState.AUTHENTICATED))
        //}

        onView(withId(R.id.reminderssRecyclerView)).check(RecyclerViewItemCountAssertion(0))
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun clickFABAndNavigateToSaveReminder() = runBlockingTest {
        repository.saveReminder(makeReminder("Reminder One", "A description", "...."))
        repository.saveReminder(makeReminder("Reminder Two", "Another reminder", "....."))

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //scenario.onFragment {
            //val res = it._viewModel.authenticationState.getOrAwaitValue()
            //assertThat(res, `is`(BaseViewModel.AuthenticationState.AUTHENTICATED))
        //}

        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB))
                .perform(click())

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun checkHandleError() = runBlockingTest {
        (repository as FakeRemindersRepository).throwErrors = true

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //scenario.onFragment {
            //val res = it._viewModel.authenticationState.getOrAwaitValue()
            //assertThat(res, `is`(BaseViewModel.AuthenticationState.AUTHENTICATED))
        //}

        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(FakeRemindersRepository.CONTRIVED_ERROR_MESSAGE)))

        (repository as FakeRemindersRepository).throwErrors = false
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