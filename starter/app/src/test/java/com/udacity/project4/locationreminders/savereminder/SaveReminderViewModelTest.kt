package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class SaveReminderViewModelTest {
    private lateinit var dataSource: FakeDataSource

    private lateinit var saveViewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @After
    fun down() {
        GlobalContext.stopKoin()
    }

    @Before
    fun prepareDate() {
        val reminderDataItem = mutableListOf<ReminderDTO>()
        dataSource = FakeDataSource(reminderDataItem)
        saveViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }


    @Test
    fun validateEnteredData() {
        val reminder1 = ReminderDataItem(null, "desc", "loc", 1.0, 1.0)
        val result1 = saveViewModel.validateEnteredData(reminder1)
        assertThat(result1, `is`(false))

        val reminder2 = ReminderDataItem("title", "desc", "loc", 2.0, 2.0)
        val result2 = saveViewModel.validateEnteredData(reminder2)
        assertThat(result2, `is`(true))
    }

    @Test
    fun onClear() = runTest{
        saveViewModel.reminderTitle.value = "test"
        saveViewModel.onClear()
        assertThat(saveViewModel.reminderTitle.getOrAwaitValue(), nullValue())
    }

    @Test
    fun saveReminder()= runTest{
        mainCoroutineRule.pauseDispatcher()
        val reminder = ReminderDataItem("title", "desc", "loc", 2.0, 2.0)
        saveViewModel.saveReminder(reminder)
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveViewModel.showToast.getOrAwaitValue(),
            `is`("Reminder Saved !")
        )
    }

}