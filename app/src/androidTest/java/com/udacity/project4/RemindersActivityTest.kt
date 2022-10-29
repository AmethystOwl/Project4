import android.app.Activity
import android.app.Application
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.MainCoroutineRule
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadow.*

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var db: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun init() = runTest {
        appContext = getApplicationContext()
        // ********* doesn't work ***********
        /* val myModule = module {
             viewModel {
                 RemindersListViewModel(
                     appContext,
                     get() as ReminderDataSource
                 )
             }
             single {
                 SaveReminderViewModel(
                     appContext,
                     get() as ReminderDataSource
                 )
             }
             single { RemindersLocalRepository(get()) }
             single { LocalDB.createRemindersDao(appContext) }
         }

         startKoin {
             modules(listOf(myModule))
         }
         repository = get()
         */

        db = Room.inMemoryDatabaseBuilder(appContext, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
        db.clearAllTables()

        repository = RemindersLocalRepository(db.reminderDao())
        saveReminderViewModel = SaveReminderViewModel(appContext, repository)
        remindersListViewModel = RemindersListViewModel(appContext, repository)
        repository.deleteAllReminders()
    }


    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    // tests for successful insertion, and checks Snackbar visibility.
    @Test
    fun testSuccessfulInsertion() { //= runBlocking {
        remindersListViewModel.deleteAll()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(
            ViewActions.typeText("My Reminder"), ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("My Description"), ViewActions.closeSoftKeyboard()
        )

        onView(withId(R.id.selectLocation)).perform(click())
        // a little delay because map initialization can take up some time.
        onView(withId(R.id.saveButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        runBlocking { delay(1000) }
        onView(withId(R.id.mapView)).perform(click())
        runBlocking { delay(1000) }
        onView(withId(R.id.saveButton)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
       // onView(withText("Reminder Saved !")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(handleActivityScenario(activityScenario).window.decorView))))
            .check(matches(isDisplayed()))
        onView(withText("My Reminder")).check(matches(isDisplayed()))
        onView(withText("My Description")).check(matches(isDisplayed()))
        activityScenario.close()
    }

    // Test that makes sure "Please enter title" snackbar is shown when it's blank
    @Test
    fun testNoTitleSnackBar() = runBlocking {
        remindersListViewModel.deleteAll()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("Description"), ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.selectLocation)).perform(click())
        delay(1000)
        onView(withId(R.id.mapView)).perform(click())
        delay(1000)
        onView(withId(R.id.saveButton)).perform(click())

        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText("Please enter title")).check(matches(isDisplayed()))

        activityScenario.close()
    }

    // Test that makes sure "Please select location" snackbar is shown when it's blank
    @Test
    fun testNoLocationSnackBar() {
        remindersListViewModel.deleteAll()
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(
            ViewActions.typeText("reminder"), ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("desc"), ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText("Please select location")).check(matches(isDisplayed()))

        activityScenario.close()
    }
    private fun handleActivityScenario(activityScenario: ActivityScenario<RemindersActivity>): Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}