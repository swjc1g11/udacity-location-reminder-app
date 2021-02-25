package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminderItem = ReminderDataItem(
                    title = title,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude
            )

            val isValidReminder = _viewModel.validateEnteredData(reminderItem)

            if (isValidReminder) {
                saveReminderAndAddGeofence(reminderItem)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    private fun saveReminderAndAddGeofence(reminderItem: ReminderDataItem) {
        if (areForegroundAndBackgroundLocationPermissionsGranted()) {
            val geofence = Geofence.Builder()
                    .setRequestId(reminderItem.id)
                    .setCircularRegion(reminderItem.latitude!!,
                            reminderItem.longitude!!,
                            100f // Radius in meters
                    )
                    .setExpirationDuration(TimeUnit.HOURS.toMillis(24 * 7 * 52))
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()

            val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

            val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
            intent.putExtra(REMINDER_ITEM_UUID_KEY, reminderItem.id)
            val geofencePendingIntent = PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(reminderItem)
                }
                addOnFailureListener {
                    val message = if (it.message != null) it.message else "-100"
                    val temp = message!!.replace(": ", "")

                    val errorCode = when (Integer.parseInt(temp)) {
                        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> R.string.enable_google_location_services
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> R.string.too_many_geofences
                        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> R.string.too_many_geofences
                        else -> R.string.unknown_error
                    }
                    _viewModel.showSnackBarInt.value = errorCode
                }
            }
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    companion object {
        const val REMINDER_ITEM_UUID_KEY = "REMINDER_ITEM_UUID_KEY"
    }
}
