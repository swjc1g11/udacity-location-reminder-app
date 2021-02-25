package com.udacity.project4.util

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.utils.FirebaseUserLiveData
import org.mockito.Mockito

class FakeFirebaseUserLiveData() : LiveData<FirebaseUser?>() {

    override fun getValue(): FirebaseUser? {
        return Mockito.mock(FirebaseUser::class.java)
    }
}