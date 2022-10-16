package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )
        val reminderDataItem =
            intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem
        binding.reminderDataItem = reminderDataItem

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment

        mapFragment.getMapAsync {
            it.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        reminderDataItem.latitude!!,
                        reminderDataItem.longitude!!
                    ), 13f
                )
            )
            it.addMarker(
                MarkerOptions().position(
                    LatLng(
                        reminderDataItem.latitude!!,
                        reminderDataItem.longitude!!
                    )
                )
            )
            /*    val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(reminderDataItem.latitude!!, reminderDataItem.longitude!!))
                    .zoom(17f)
                    .bearing(90f)
                    .build()
                it.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))*/
        }
    }
}
