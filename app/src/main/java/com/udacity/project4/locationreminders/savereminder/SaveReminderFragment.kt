package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

private const val TAG = "SaveReminderFragment"

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    private val backgroundLocationContent =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when (it) {
                false -> {
                    Snackbar.make(
                        requireView(),
                        "Background permission is needed for geofencing",
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
                true -> {
                    val title = _viewModel.reminderTitle.value
                    val description = _viewModel.reminderDescription.value
                    val locationStr = _viewModel.reminderSelectedLocationStr.value
                    val latitude = _viewModel.latitude.value
                    val longitude = _viewModel.longitude.value
                    addGeoFenceAndSaveToDb(latitude, longitude, title, description, locationStr)
                }
            }
        }
    private val locationSettingsContent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val locationStr = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value

                addGeoFenceAndSaveToDb(latitude, longitude, title, description, locationStr)
            }
        }

    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @SuppressLint("InlinedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding.saveReminder.setOnClickListener {
            _viewModel.reminderTitle.value = binding.reminderTitle.text.toString()
            _viewModel.reminderDescription.value = binding.reminderDescription.text.toString()
            if (!backgroundLocationApproved()) {
                backgroundLocationContent.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val locationStr = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value

                addGeoFenceAndSaveToDb(latitude, longitude, title, description, locationStr)
            }
        }
    }

    private fun backgroundLocationApproved(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    }

    @SuppressLint("MissingPermission")
    private fun addGeoFenceAndSaveToDb(
        latitude: Double?,
        longitude: Double?,
        title: String?,
        description: String?,
        locationStr: String?
    ) {
        if (isForegroundPermissionApproved()) {
            if (isBackgroundPermissionApproved()) {
                val locationRequest = LocationRequest.create()
                locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
                val locationSettingsBuilder =
                    LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                val locationServices = LocationServices.getSettingsClient(requireActivity())
                locationServices.checkLocationSettings(locationSettingsBuilder.build())
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val reminderDataItem =
                                ReminderDataItem(
                                    title,
                                    description,
                                    locationStr,
                                    latitude,
                                    longitude
                                )
                            if (_viewModel.validateEnteredData(reminderDataItem)) {
                                val geofence = Geofence.Builder().setRequestId(reminderDataItem.id)
                                    .setCircularRegion(
                                        reminderDataItem.latitude!!,
                                        reminderDataItem.longitude!!,
                                        200f
                                    )
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

                                val geofencingRequest = GeofencingRequest.Builder()
                                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                    .addGeofence(geofence)
                                    .build()

                                geofencingClient.addGeofences(
                                    geofencingRequest,
                                    geofencePendingIntent
                                ).run {
                                    addOnSuccessListener {
                                        Log.d("Add Geofence", geofence.requestId)
                                    }
                                    addOnFailureListener {
                                        if ((it.message != null)) {
                                            Log.d(TAG, it.message!!)
                                        }
                                    }
                                }
                                _viewModel.validateAndSaveReminder(reminderDataItem)
                            }
                        }
                    }.addOnFailureListener {
                        if (it is ResolvableApiException) {
                            try {
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(it.resolution).build()
                                locationSettingsContent.launch(intentSenderRequest)
                            } catch (sendEx: IntentSender.SendIntentException) {
                                Log.d(
                                    TAG,
                                    "Error getting location settings resolution: " + sendEx.message
                                )
                            }
                        } else {
                            Snackbar.make(
                                requireView(),
                                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                            ).setAction(android.R.string.ok) {
                                addGeoFenceAndSaveToDb(
                                    latitude,
                                    longitude,
                                    title,
                                    description,
                                    locationStr
                                )
                            }.show()

                        }
                    }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }


    private fun isBackgroundPermissionApproved(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isForegroundPermissionApproved(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

