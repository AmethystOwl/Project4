package com.udacity.project4.locationreminders.reminderslist
import MainCoroutineRule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var repository: FakeDataSource


    @Before
    fun init() {
        stopKoin()
        repository = FakeDataSource()
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), repository)
    }
    @After
    fun teardown() = runTest {
        repository.deleteAllReminders()
    }


    @Test
    fun testLoading() = runTest {
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertEquals(true,remindersListViewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        assertEquals(false,remindersListViewModel.showLoading.getOrAwaitValue())
    }
    @Test
    fun testHasData() = runTest{
        val reminderDTO = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            longitude = 0.0,
            latitude = 0.0
        )
        repository.saveReminder(reminderDTO)
        remindersListViewModel.loadReminders()
        assertEquals(1,remindersListViewModel.remindersList.getOrAwaitValue().size)


    }
    @Test
    fun testNoData() = runTest {
        repository.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertEquals(true,remindersListViewModel.showNoData.getOrAwaitValue())
    }

    @Test
    fun testSnackBarError() = runTest{
        repository.setReturnError(true)
        remindersListViewModel.loadReminders()
        assertEquals("Test exception",remindersListViewModel.showSnackBar.getOrAwaitValue())

    }

}
