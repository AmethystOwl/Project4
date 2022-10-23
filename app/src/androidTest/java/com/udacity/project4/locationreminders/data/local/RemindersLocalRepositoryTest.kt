package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: ReminderDataSource
    private lateinit var database: RemindersDatabase


    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        remindersRepository = RemindersLocalRepository(database.reminderDao())
    }
    // Tries to insert a reminder into the database using the repository,
    // then fetches them using ids, and then compares them against the original items.
    @Test
    fun testInsertReminder() = runBlocking {
        val reminder1 = ReminderDTO(
            "title",
            "desc",
            "loc",
            1.0,
            2.0
        )
        val reminder2 = ReminderDTO(
            "title2",
            "desc2",
            "loc2",
            1.0,
            2.0
        )
        remindersRepository.saveReminder(reminder1)
        remindersRepository.saveReminder(reminder2)
        Assert.assertEquals(
            reminder1,
            ((remindersRepository.getReminder(reminder1.id) as Result.Success).data)
        )
        Assert.assertEquals(
            reminder2,
            ((remindersRepository.getReminder(reminder2.id) as Result.Success).data)
        )

    }
    // Tries to insert a reminder into the database using the repository, then fetches it using id, and then compares the ids.
    @Test
    fun testGetReminderById() = runBlocking {
        val reminder1 = ReminderDTO(
            "title",
            "desc",
            "loc",
            1.0,
            2.0
        )
        remindersRepository.saveReminder(reminder1)
        Assert.assertEquals(
            reminder1,
            ((remindersRepository.getReminder(reminder1.id) as Result.Success).data)
        )


    }
    // Tries to empty the database using the repository, then fetches all data, and ensures that retrieved list is empty.
    @Test
    fun testDeletionAll() = runBlocking {
        val reminder1 = ReminderDTO(
            "title",
            "desc",
            "loc",
            1.0,
            2.0
        )
        val reminder2 = ReminderDTO(
            "title",
            "desc",
            "loc",
            1.0,
            2.0
        )
        remindersRepository.saveReminder(reminder1)
        remindersRepository.saveReminder(reminder2)
        remindersRepository.deleteAllReminders()
        assertThat((remindersRepository.getReminders() as Result.Success).data.size, `is`(0))

    }

    @After
    fun clearDatabase() = runTest {
        database.close()
    }
}
