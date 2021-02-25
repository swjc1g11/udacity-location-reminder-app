package com.udacity.project4.base

import android.Manifest
import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    companion object {
        const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 1000
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 1001
    }

    override fun onStart() {
        super.onStart()

        _viewModel.showErrorMessage.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showToast.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            Snackbar.make(this.view!!, it, Snackbar.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            Snackbar.make(this.view!!, getString(it), Snackbar.LENGTH_LONG).show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (
                grantResults.isEmpty() ||
                grantResults[0] == PackageManager.PERMISSION_DENIED)
        {
            // Permission denied.
            // Call show enable location dialogue from BaseFragment
            if (!shouldShowRequestPermissionRationale(permissions[0])) {
                showEnableLocationAlertDialog()
            }
        } else if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                grantResults[1] ==
                PackageManager.PERMISSION_DENIED) {

            if (!shouldShowRequestPermissionRationale(permissions[1])) {
                showEnableLocationAlertDialog()
            } else {
                Snackbar.make(requireView(), R.string.requires_background_permission, Snackbar.LENGTH_LONG)
                        .show()
            }
        }
    }

    @TargetApi(29)
    protected fun areForegroundAndBackgroundLocationPermissionsGranted() : Boolean {

        val foregroundLocationApproved = isForegroundPermissionEnabled()

        // If running android 10 or later, it's necessary to request background location permissions, too
        // Otherwise it is safe to assume the app can access background location information
        val backgroundPermissionApproved = isBackgroundPermissionEnabled()
        // Return true only if both permissions are approved
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    protected fun isForegroundPermissionEnabled(): Boolean {
        return (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(requireActivity(),
                                Manifest.permission.ACCESS_FINE_LOCATION))
    }

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

    @TargetApi(29)
    protected fun requestForegroundAndBackgroundLocationPermissions() {
        var permissionsArray = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            // If running android 10 or later, add the background location permission as one to request
            // Give back a different result code
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        requestPermissions(
                permissionsArray,
                resultCode
        )
    }

    protected fun showEnableLocationAlertDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder
                .setTitle(R.string.location_required_title)
                .setMessage(R.string.location_required_error)
                .setPositiveButton(R.string.location_required_enablelocation) { dialog: DialogInterface?, which: Int ->
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, which: Int ->
                    dialog?.let {
                        dialog.dismiss()
                    }
                }
                .show()
    }
}