package com.siroha.gamespace.feature.blocker

import android.telecom.Call
import android.telecom.CallScreeningService
import com.siroha.gamespace.core.blocker.BlockerSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlockerCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var blockerSettingsStore: BlockerSettingsStore

    override fun onScreenCall(callDetails: Call.Details) {
        val response = if (blockerSettingsStore.isCallBlockingActive) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false) // still shows in call log — blocked, not hidden
                .setSkipNotification(true)
                .build()
        } else {
            CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
    }
}
