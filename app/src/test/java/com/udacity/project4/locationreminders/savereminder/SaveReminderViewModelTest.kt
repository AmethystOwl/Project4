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