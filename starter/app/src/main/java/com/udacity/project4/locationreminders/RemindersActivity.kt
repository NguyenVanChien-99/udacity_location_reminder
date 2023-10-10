package com.udacity.project4.locationreminders

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.databinding.ActivityRemindersBinding

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(AuthUI.getInstance().auth.currentUser==null){
            Log.i("main activity", "onCreate: unauthenticated")
            //TODO move to login activity
        }
//        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
//        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                (binding.navHostFragment as NavHostFragment).navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
