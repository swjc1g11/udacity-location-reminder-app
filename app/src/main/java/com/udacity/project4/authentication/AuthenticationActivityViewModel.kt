package com.udacity.project4.authentication

import android.app.Application
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.utils.FirebaseUserLiveData

class AuthenticationActivityViewModel(
    override val app: Application
) : BaseViewModel(app) {}