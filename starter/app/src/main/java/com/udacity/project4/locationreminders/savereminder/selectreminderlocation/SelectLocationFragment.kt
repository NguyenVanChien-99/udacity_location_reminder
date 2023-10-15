package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.UUID

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var selectedPOI: PointOfInterest

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    }


    private fun onLocationSelected() {
        if (!this::selectedPOI.isInitialized) {
            Toast.makeText(requireActivity(), "Select something first", Toast.LENGTH_SHORT).show();
        } else {
            _viewModel.selectedPOI.value = selectedPOI
            _viewModel.reminderSelectedLocationStr.value = selectedPOI.name
            _viewModel.latitude.value = selectedPOI.latLng.latitude
            _viewModel.longitude.value = selectedPOI.latLng.longitude
            findNavController().popBackStack()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.uiSettings.isZoomControlsEnabled=true
        setMapStyle(map)
        setOnClick(map)
        checkUserPermission()

    }

    private fun setOnClick(map: GoogleMap) {
        map.setOnMapClickListener { pos ->
            map.clear()
            _viewModel.onClear()

            val latLng = LatLng(pos.latitude, pos.longitude)
            val title = "Selected one"
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
            )
            marker?.let {
                it.showInfoWindow()
            }

            selectedPOI = PointOfInterest(latLng, UUID.randomUUID().toString(), title)
        }

        map.setOnPoiClickListener { poi ->

            map.clear()
            _viewModel.onClear()

            val marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            marker?.let {
                it.showInfoWindow()
            }

            selectedPOI = poi
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style)
            )
            if (!success) {
                Log.i("setMapStyle", "setMapStyle: failed")
            }
        } catch (ex: Exception) {
            Log.i("setMapStyle", "setMapStyle: error ${ex}")
        }
    }

    private val requestPermissionLauncher =registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted : Boolean->
        if (isGranted){
            checkUserPermission()
        }else{
            _viewModel.showToast.value= getString(R.string.permission_denied_explanation)
        }
    }

    private fun checkUserPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                val locationRequest = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_LOW_POWER
                }
                val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                val settingsClient = LocationServices.getSettingsClient(requireActivity())
                val locationSettingsResponseTask =
                    settingsClient.checkLocationSettings(builder.build())

                locationSettingsResponseTask.addOnFailureListener { exception ->
                    if (exception is ResolvableApiException){
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
//                    exception.startResolutionForResult(requireActivity(),
//                        REQUEST_TURN_DEVICE_LOCATION_ON)
                            startIntentSenderForResult(exception.resolution.intentSender,
                                29,null,0,0,0,null)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Log.d("checkDeviceLocationSettingsAndStartGeofence", "Error geting location settings resolution: " + sendEx.message)
                        }
                    } else {
                        Snackbar.make(
                            requireView(),
                            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                        ).setAction(android.R.string.ok) {
//                            checkDeviceLocationSettingsAndStartGeofence()
                        }.show()
                    }
                }
                Log.i("PERMISSION", "checkNotificationPermission: granted")
                map.isMyLocationEnabled = true
                //move our screen to my location
                fusedLocationClient.lastLocation.addOnSuccessListener { locate: Location? ->
                    if (locate != null) {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    locate.latitude,
                                    locate.longitude
                                ), 15f
                            )
                        )
                    }
                }
                return
            }

            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}