package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun prepareData() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }
    @Test
    fun insert() = runTest  {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder)
        val resp = database.reminderDao().getReminderById(reminder.id)
        resp?.let {
            assertThat(resp.id, `is`(reminder.id))
        }
    }

    @Test
    fun getAll() = runTest {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder)
        val resp = database.reminderDao().getReminders()
        assertThat(resp.size, `is`(1))
    }

    @Test
    fun deleteAll() = runTest {
        val reminder = ReminderDTO("title", "desc", "loc", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()
        val resp = database.reminderDao().getReminders()
        assertThat(resp.size, `is`(0))
    }

}