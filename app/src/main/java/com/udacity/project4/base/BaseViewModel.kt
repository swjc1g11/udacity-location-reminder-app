package com.udacity.project4.base

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.utils.FirebaseUserLiveData
import com.udacity.project4.utils.SingleLiveEvent
import org.koin.android.ext.android.getKoin
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.dsl.koinApplication

/**
 * Base class for View Models to declare the common LiveData objects in one place
 */
abstract class BaseViewModel(
    open val app: Application
) : AndroidViewModel(app), KoinComponent {

    val navigationCommand: SingleLiveEvent<NavigationCommand> = SingleLiveEvent()
    val showErrorMessage: SingleLiveEvent<String> = SingleLiveEvent()
    val showSnackBar: SingleLiveEvent<String> = SingleLiveEvent()
    val showSnackBarInt: SingleLiveEvent<Int> = SingleLiveEvent()
    val showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showLoading: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val showNoData: MutableLiveData<Boolean> = MutableLiveData()

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    private val auth: LiveData<FirebaseUser?> by inject()

    val authenticationState = Transformations.map(auth) { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

}