package com.udacity.project4

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(maxSdkVersion = 29)
//END TO END test to black box test the app
class RemindersActivityTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var datasource: ReminderDataSource
    private lateinit var context: Application

    @Before
    fun init() {
        //stop the original app koin
        stopKoin()
        context = getApplicationContext()
        val myModule = module {
            viewModel { RemindersListViewModel(context, get() as ReminderDataSource) }
            single { SaveReminderViewModel(context, get() as ReminderDataSource) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(context) }
        }
        // new koin module
        startKoin { modules(listOf(myModule)) }
        //Get our repository
        datasource = get().get()
        // delete the data to start fresh
        runBlocking { datasource.deleteAllReminders() }
    }

    @After
    fun down() {
        stopKoin()
    }
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @After
    fun unregisterResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Before
    fun registerResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun endToEndTest() {

        val expected = ReminderDTO("title", "desc", "loc", 1.0, 1.0)

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        //Show no data
        onView(withText(R.string.no_data)).check(matches(isDisplayed()))

        //Save reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.selectLocation)).perform(click())
        Thread.sleep(2000)
        //choose location
        onView(withId(R.id.map)).perform(click(pressBack()))
        Thread.sleep(1000)
        onView(withId(R.id.btnSave)).perform(click())
        Thread.sleep(1000)
        //fill tile
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(expected.title))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(expected.description))
        Espresso.closeSoftKeyboard()
        Thread.sleep(1000)
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(getActivity(scenario)?.window?.decorView)))).check(matches(isDisplayed()))
        onView(withText(expected.title)).check(matches(isDisplayed()))
        onView(withText(expected.description)).check(matches(isDisplayed()))
        scenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    @Test
    fun endToEndTest_WithoutLocation() {

        val expected = ReminderDTO("title", "desc", "loc", 1.0, 1.0)

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(scenario)
        //Show no data
        onView(withText(R.string.no_data)).check(matches(isDisplayed()))

        //Save reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        Thread.sleep(1000)
        //fill tile
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(expected.title))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(expected.description))
        Espresso.closeSoftKeyboard()
        Thread.sleep(1000)
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))
    }

    @Test
    fun endToEndTest_WithoutTitle() {

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(scenario)
        //Show no data
        onView(withText(R.string.no_data)).check(matches(isDisplayed()))

        //Save reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.selectLocation)).perform(click())
        Thread.sleep(2000)
        //choose location
        onView(withId(R.id.map)).perform(click(pressBack()))
        Thread.sleep(1000)
        onView(withId(R.id.btnSave)).perform(click())
        Thread.sleep(1000)
        //fill tile
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
    }
}
