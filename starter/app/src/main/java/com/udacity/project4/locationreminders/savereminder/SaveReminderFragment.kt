package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var itemSelected: ReminderDataItem

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        checkNotificationPermission()
        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value


            itemSelected = ReminderDataItem(title, description, location, latitude, longitude)
            checkPermissionsAndStartGeofencing()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
//        Log.i("onDestroy", "onDestroy: desssssss")
//        removeGeofences()
    }

//    @TargetApi(29)
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            Log.i("TAG", "checkPermissionsAndStartGeofencing: ok")
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            Log.i("TAG", "checkPermissionsAndStartGeofencing: no its not")
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

//    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

//    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
    Log.i("requestForegroundAndBackgroundLocationPermissions", "requestForegroundAndBackgroundLocationPermissions: required")
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            1
        )
    }


    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("checkDeviceLocationSettingsAndStartGeofence", "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForClue()
            }
        }
    }

    private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences removed
                Log.i("removeGeofences", "removeGeofences: removed")
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.i("removeGeofences", "error ${it.message}")
            }
        }
    }

    private fun addGeofenceForClue(){

        if(!this::itemSelected.isInitialized||itemSelected.latitude ==null ||itemSelected.longitude==null){
            Log.i("addGeofenceForClue", "addGeofenceForClue: return null")
            return
        }

        // Build the Geofence Object
        val geofence = Geofence.Builder()
            // Set the request ID, string to identify the geofence.
            .setRequestId(itemSelected.id)
            // Set the circular region of this geofence.
            .setCircularRegion(
                itemSelected.latitude!!,
                itemSelected.longitude!!,
                2000f
            )
            // Set the expiration duration of the geofence. This geofence gets
            // automatically removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            // Add the geofences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        Log.i("addGeofenceForClue", "addGeofenceForClue: built")
        // First, remove any existing geofences that use our pending intent
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            // Regardless of success/failure of the removal, add the new geofence
            addOnCompleteListener {
                Log.i("addGeofenceForClue", "addGeofenceForClue: removed")
                // Add the new geofence request with the new geofence
                if (ActivityCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                        addOnSuccessListener {
                            Log.i("Add Geofence", geofence.requestId)
                            _viewModel.validateAndSaveReminder(itemSelected)
                            _viewModel.showToast.value="Geofence added."
                        }
                        addOnFailureListener {
                            if ((it.message != null)) {
                                Log.i("Add geofence", "addGeofenceForClue: ${it.message}")
                            }
                        }
                    }
                }else{
                    Log.i("addGeofenceForClue", "addGeofenceForClue: missing permission")
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            // Permission denied.
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", "com.udacity.project4", null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private fun checkNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("PERMISSION", "checkNotificationPermission: granted")
                return
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1