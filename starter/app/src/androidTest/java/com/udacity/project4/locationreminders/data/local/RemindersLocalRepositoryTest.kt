package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun prepareData() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @Test
    fun insert() = runTest  {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        repository.saveReminder(reminder)
        val resp = repository.getReminder(reminder.id) as Result.Success
        resp?.let {
            assertThat(resp.data.id, `is`(reminder.id))
        }
    }

    @Test
    fun getAll() = runTest {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        repository.saveReminder(reminder)
        val resp = repository.getReminders() as Result.Success
        assertThat(resp.data.size, `is`(1))
    }

    @Test
    fun deleteAll() = runTest {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        repository.saveReminder(reminder)
        repository.deleteAllReminders()
        val resp = repository.getReminders() as Result.Success
        assertThat(resp.data.size, `is`(0))
    }
}