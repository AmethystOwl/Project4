package com.udacity.project4.locationreminders.savereminder

import MainCoroutineRule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.util.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: FakeDataSource
    private lateinit var saveReminderViewModel : SaveReminderViewModel

    @Before
    fun setup() {
        stopKoin()
        repository = FakeDataSource()
        saveReminderViewModel =SaveReminderViewModel(ApplicationProvider.getApplicationContext(), repository)
    }

    @After
    fun teardown() = runTest {
        saveReminderViewModel.onClear()
        repository.deleteAllReminders()

    }
    // sets location to null to check if the 'showSnackBarInt' livedata value is changed to err_select_location string resource value.
    @Test
    fun testNoLocationReminder() {
        val reminderDataItem = ReminderDataItem(
            title = "title",
            description = "desc",
            location = null,
            longitude = 0.0,
            latitude = 0.0
        )
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        val result = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertEquals(R.string.err_select_location, result)


    }
    // sets title to null to check if the 'showSnackBarInt' livedata value is changed to err_enter_title string resource value.
    @Test
    fun testNoTitleReminder() {
        val reminderDataItem = ReminderDataItem(
            title = null,
            description = "desc",
            location = "loc",
            longitude = 0.0,
            latitude = 0.0
        )
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        val result = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertEquals(R.string.err_enter_title, result)
    }
    // Inserts a valid Reminder to check if the 'showToast' livedata value is changed "Reminder Saved !".
    @Test
    fun testSavedSuccessfully() {
        val reminderDataItem = ReminderDataItem(
            title = "title",
            description = "desc",
            location = "loc",
            longitude = 0.0,
            latitude = 0.0
        )
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        val result = saveReminderViewModel.showToast.getOrAwaitValue()
        assertEquals("Reminder Saved !", result)
    }

    // Inserts a valid Reminder to check if the 'showLoading' livedata value is changed to TRUE when it's inserting, and FALSE when it's done.
    @Test
    fun testLoading() {
        val reminderDataItem = ReminderDataItem(
            title = "title",
            description = "desc",
            location = "loc",
            longitude = 0.0,
            latitude = 0.0
        )
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

        assertEquals(true, saveReminderViewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()

        assertEquals(false, saveReminderViewModel.showLoading.getOrAwaitValue())
    }

    // Checks if 'navigationCommand' livedata value is set to NavigationCommand.Back after saving a reminder to navigate back.
    @Test
    fun testNavigationBack() {
        val reminderDataItem = ReminderDataItem(
            title = "title",
            description = "desc",
            location = "loc",
            longitude = 0.0,
            latitude = 0.0
        )
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        assertEquals(
            NavigationCommand.Back,
            saveReminderViewModel.navigationCommand.getOrAwaitValue()
        )

    }
}
