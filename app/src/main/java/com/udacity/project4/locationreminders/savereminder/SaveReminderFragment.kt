package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.annotation.TargetApi
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
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

//const variables for the code
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val GEOFENCE_RADIUS_IN_METERS = 100f

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {

    private val geofencePendingIntent: PendingIntent by lazy {

        // intent object to connect to the GeofenceBroadcastReceiver
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)

        //set the intent action
        intent.action = ACTION_GEOFENCE_EVENT

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
        else
            PendingIntent.getBroadcast(requireContext(), 0, intent,0)    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    //variables for the code
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        //instance of the GeofencingClient
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
            val description = _viewModel.reminderDescription
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude
            val longitude = _viewModel.longitude.value

            reminderDataItem =
                ReminderDataItem(title, description.value, location, latitude.value, longitude)

            if (_viewModel.validateEnteredData(reminderDataItem)) {

                if (foregroundAndBackgroundLocationPermissionApproved()) {

                    checkDeviceLocationSettingsAndStartGeofence()

                } else {

                    requestForegroundAndBackgroundLocationPermissions()

                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    // @RequiresApi was (29) but I change it to "Build.VERSION_CODES.Q" as onViewCreated
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {

        //check if the foreground permission Approved
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
                ))

        //check if the background permission Approved
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }



        return foregroundLocationApproved && backgroundPermissionApproved
    }

    //explained in the body
    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestForegroundAndBackgroundLocationPermissions() {

        //check if foreground And Background Location Permission Approved
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        //array of permissions contains location permission
        var permissionsArray = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)

        //identify the permission request
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray = arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }



        /*        //requests the permissions
        ActivityCompat.requestPermissions(
            this@SaveReminderFragment.requireActivity(),
            permissionsArray,
            resultCode
        )*/
        this.requestPermissions(permissionsArray, resultCode)


    }

    //responds to a permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_DENIED)
        ){
            Snackbar.make(
                binding.progressBar,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else checkDeviceLocationSettingsAndStartGeofence()

    }

    //check the device's location settings and start Geofence
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())

        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->

            if (exception is ResolvableApiException && resolve) {
                try {

                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }

            } else {

                Snackbar.make(
                    binding.progressBar,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()

            }
        }

        locationSettingsResponseTask.addOnCompleteListener {

            if (it.isSuccessful) {
                geofencing()
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // Check if the user enabled location services.
            if (resultCode == Activity.RESULT_OK) {
                // Location services are enabled, so start the geofence.
                checkDeviceLocationSettingsAndStartGeofence(false)
            } else {
                // Location services are not enabled, so show a snackbar to the user.
                Snackbar.make(
                    binding.progressBar,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
    }

    //creating the geofence and setting its value
    @SuppressLint("MissingPermission")
    private fun geofencing() {

        //creating a Geofence that never expired and only triggered if enter the pinned location
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        //GeofencingRequest object
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        //set the geo data to the data object
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {

            addOnSuccessListener {
                _viewModel.saveReminder(reminderDataItem)
            }

            addOnFailureListener {
                _viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            }

        }
    }

}