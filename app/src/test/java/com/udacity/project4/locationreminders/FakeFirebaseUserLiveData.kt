package com.udacity.project4.locationreminders

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.utils.FirebaseUserLiveData
import org.mockito.Mockito.mock

class FakeFirebaseUserLiveData() : LiveData<FirebaseUser?>() {

    override fun getValue(): FirebaseUser? {
        return mock(FirebaseUser::class.java)
    }
}