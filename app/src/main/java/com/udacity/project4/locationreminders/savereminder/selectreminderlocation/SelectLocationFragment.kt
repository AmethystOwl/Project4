package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment() {
    private var poiMarker: Marker? = null
    private var mPoi: PointOfInterest? = null

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var mapView: GoogleMap

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment

        mapFragment.getMapAsync {
            if (foregroundPermissionApproved()) {
                mapView = it

                val locationManager =
                    requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val criteria = Criteria()

                val location = locationManager.getLastKnownLocation(
                    locationManager.getBestProvider(
                        criteria,
                        false
                    )!!
                )
                if (location != null) {
                    mapView.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ), 13f
                        )
                    )

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(17f)
                        .bearing(90f)
                        .build()
                    mapView.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }


                setMapStyle(mapView)
                mapView.setOnPoiClickListener { poi ->
                    mPoi = poi
                    poiMarker?.remove()
                    poiMarker = null
                    poiMarker = mapView.addMarker(
                        MarkerOptions()
                            .position(poi.latLng)
                            .title(poi.name)

                    )
                    poiMarker?.showInfoWindow()
                    binding.saveButton.visibility = View.VISIBLE

                }
                mapView.setOnMapClickListener {
                   val poi =  PointOfInterest(it,"custom","custom")
                    mPoi = poi
                    poiMarker?.remove()
                    poiMarker = null
                    poiMarker = mapView.addMarker(
                        MarkerOptions()
                            .position(poi.latLng)
                            .title(poi.name)

                    )
                    poiMarker?.showInfoWindow()
                    binding.saveButton.visibility = View.VISIBLE
                }
            }
        }
        binding.saveButton.setOnClickListener {
            onLocationSelected()

        }



        return binding.root
    }


    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun onLocationSelected() {
        if (poiMarker != null && mPoi != null) {
            _viewModel.selectedPOI.value = mPoi
            _viewModel.latitude.value = mPoi?.latLng?.latitude
            _viewModel.longitude.value = mPoi?.latLng?.longitude
            _viewModel.reminderSelectedLocationStr.value = poiMarker?.title
            findNavController().popBackStack()

        }

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            mapView.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mapView.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mapView.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mapView.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofencing()
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: background approved")
            } else {
                Log.d(TAG, "onRequestPermissionsResult: background rejected")
            }
        } else if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mapView.isMyLocationEnabled = true

            }
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionsApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundPermissionApproved()) {
            if (backgroundPermissionApproved()) {
                return
            } else {
                requestBackgroundPermission()
            }
        } else {
            requestForegroundPermission()
        }
    }

    @TargetApi(29)
    private fun requestForegroundPermission() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(
            requireActivity(),
            perms,
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    @TargetApi(29)
    private fun requestBackgroundPermission() {
        val perms = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ActivityCompat.requestPermissions(
            requireActivity(),
            perms,
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
        )
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionsApproved(): Boolean {
        return foregroundPermissionApproved() && backgroundPermissionApproved()
    }


    private fun foregroundPermissionApproved(): Boolean {
        return ((ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED))
    }

    @TargetApi(29)
    private fun backgroundPermissionApproved(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON && resultCode == Activity.RESULT_CANCELED) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }else{
            mapView.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
        val locationSettingsBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val locationServices = LocationServices.getSettingsClient(requireActivity())
        locationServices.checkLocationSettings(locationSettingsBuilder.build())
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    mapView.isMyLocationEnabled = true

                }
            }.addOnFailureListener {
                if (it is ResolvableApiException && resolve) {
                    try {
                        it.startResolutionForResult(
                            requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
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
    }

}
