package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var remindersListViewModel: RemindersListViewModel

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var repository: FakeDataSource
    private lateinit var db: RemindersDatabase

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Test
    fun testNavigationToFragment() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            dataBindingIdlingResource.monitorFragment(scenario)
            Navigation.setViewNavController(it.requireView(), navController)
        }
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())

    }


    @Before
    fun setUp() {
        stopKoin()
        db = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(getApplicationContext(), repository)

        val myModule = module {
            single {
                remindersListViewModel
            }
        }
        startKoin {
            modules(listOf(myModule))
        }
    }

    @Test
    fun testDisplayedDataOnUi() = runTest {

        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val reminderDTO = ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            longitude = 0.0,
            latitude = 0.0
        )
        repository.saveReminder(reminderDTO)
        remindersListViewModel.loadReminders()
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText(reminderDTO.title)))
        onView(withId(R.id.description)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches(withText(reminderDTO.description)))
        onView(withId(R.id.location)).check(matches(isDisplayed()))
        onView(withId(R.id.location)).check(matches(withText(reminderDTO.location)))

    }

    @Test
    fun testForNoData() = runTest{
        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        remindersListViewModel.loadReminders()
        onView(withText("No Data")).check(matches(isDisplayed()))

    }

    @Test
    fun testForErrorSnackBar() = runTest{
        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        repository.setReturnError(true)
        remindersListViewModel.loadReminders()
        onView(withText("Test exception")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    }

}