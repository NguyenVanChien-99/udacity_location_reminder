package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    private lateinit var dataSource: FakeDataSource

    private lateinit var remindersListViewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun prepareDate() {
        val reminderDataItem = mutableListOf<ReminderDTO>()
        dataSource = FakeDataSource(reminderDataItem)
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    fun showData()=mainCoroutineRule.runBlockingTest{
        dataSource.saveReminder(ReminderDTO("til","desc","loc",1.0,1.0,"1"))
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(3))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun showError()=mainCoroutineRule.runBlockingTest{
        mainCoroutineRule.pauseDispatcher()
        dataSource.returnError=true
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.showErrorMessage.getOrAwaitValue(), `is`(Exception("Get reminder error").toString()))
        dataSource.returnError=false
    }


    @Test
    fun nodata()=mainCoroutineRule.runBlockingTest{
        dataSource.deleteAllReminders()
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}