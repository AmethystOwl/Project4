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
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.android.synthetic.main.it_reminder.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment() {
    @SuppressLint("MissingPermission")
    private val requestPermsContent =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms: Map<String, Boolean> ->
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                mapView.isMyLocationEnabled = true
                checkDeviceLocationSettingsAndStartGeofence(true)
            } else {
                Snackbar.make(
                    requireView(),
                    "Location permission is needed",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("OK") {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + requireContext().packageName)
                    )
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }.show()
            }
        }

    @SuppressLint("MissingPermission")
    private val locationsServicesContent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                mapView.isMyLocationEnabled = true
            }
        }


    private var poiMarker: Marker? = null
    private var mPoi: PointOfInterest? = null

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
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
            mapView = it
            if (foregroundPermissionApproved()) {
                mapView.isMyLocationEnabled = true

            }
            val locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            var location: Location? = null
            if (foregroundPermissionApproved()) {
                location = locationManager.getLastKnownLocation(
                    locationManager.getBestProvider(
                        criteria,
                        false
                    )!!
                )
            }
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
                val poi = PointOfInterest(it, "custom", "custom")
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
            mapView.setOnMyLocationButtonClickListener(listener)

        }
        binding.saveButton.setOnClickListener {
            onLocationSelected()

        }


        return binding.root
    }

    private val listener = GoogleMap.OnMyLocationButtonClickListener {
        checkDeviceLocationSettingsAndStartGeofence()
        true
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
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


    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
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
        requestPermsContent.launch(perms)
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
                        val intentSenderRequest = IntentSenderRequest.Builder(it.resolution).build()
                        locationsServicesContent.launch(intentSenderRequest)
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
