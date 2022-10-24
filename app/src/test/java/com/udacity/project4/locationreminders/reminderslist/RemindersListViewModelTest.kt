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

    // Makes sure that showLoading livedata is true when repository is fetching data, and is false when it's done.
    @Test
    fun testLoading() = runTest {
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertEquals(true,remindersListViewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        assertEquals(false,remindersListViewModel.showLoading.getOrAwaitValue())
    }
    // Makes sure that the viewModel fetches data correctly, by inserting one item and checks if the retrieved list size is 1.
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
    // Makes sure that the 'showNoData' livedata is set to true when the database is empty.
    @Test
    fun testNoData() = runTest {
        repository.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertEquals(true,remindersListViewModel.showNoData.getOrAwaitValue())
    }
    // Makes sure that the 'showSnackBar' livedata value is equal to "Test exception" when there's an error.
    @Test
    fun testSnackBarError() = runTest{
        repository.setReturnError(true)
        remindersListViewModel.loadReminders()
        assertEquals("Error occurred while trying to retrieve reminders.",remindersListViewModel.showSnackBar.getOrAwaitValue())

    }

}
