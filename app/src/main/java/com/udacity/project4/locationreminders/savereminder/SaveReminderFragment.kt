package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
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

    companion object {
        const val REQUEST_BACKGROUND_ONLY_REQUEST_CODE = 1002
        const val REMINDER_ITEM_UUID_KEY = "REMINDER_ITEM_UUID_KEY"
    }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isBackgroundPermissionEnabled()) {
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                // _viewModel.showSnackBar.value = "Access to your location is required.";
                Snackbar.make(requireView(), getString(R.string.permission_rationale_snackbar_text), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.permission_rationale_snackbar_button_text)) {
                            requestBackgroundPermissions()
                        }
                        .show()
            } else {
                Snackbar.make(requireView(), getString(R.string.background_permissions_denied), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.change_permissions)) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }
                        .show()
            }
        }
    }

    @TargetApi(29)
    protected fun requestBackgroundPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val permissionsArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            requestPermissions(
                    permissionsArray,
                    REQUEST_BACKGROUND_ONLY_REQUEST_CODE
            )
        }
    }

    @TargetApi(29)
    protected fun isBackgroundPermissionEnabled(): Boolean {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                            requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            return true
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveReminderAndAddGeofence(reminderItem: ReminderDataItem) {
        if (isBackgroundPermissionEnabled()) {
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
            if (!isBackgroundPermissionEnabled()) {
                val builder = AlertDialog.Builder(requireActivity())
                builder
                        .setTitle(getString(R.string.background_location_required_dialogue_title))
                        .setMessage(R.string.background_location_required_dialogue_body)
                        .setPositiveButton(R.string.location_required_enablelocation) { dialog: DialogInterface?, which: Int ->
                            requestBackgroundPermissions()
                        }
                        .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, which: Int ->
                            dialog?.let {
                                dialog.dismiss()
                            }
                        }
                        .show()
            }
        }
    }
}
