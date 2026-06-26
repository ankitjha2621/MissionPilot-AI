package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.api.FirestoreService
import com.example.data.api.SavedMissionState
import com.example.ui.viewmodel.MissionPilotViewModel
import kotlinx.coroutines.launch

class WidgetsBindingObserver(
    private val context: Context,
    private val viewModel: MissionPilotViewModel,
    private val onStateResumed: (SavedMissionState) -> Unit
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("WidgetsBindingObserver", "Application lifecycle: RESUMED. Checking Firestore state...")
        
        owner.lifecycleScope.launch {
            try {
                val savedState = FirestoreService.getSavedState(context)
                if (savedState != null) {
                    val elapsedMs = System.currentTimeMillis() - savedState.saveTimestamp
                    if (elapsedMs > 5 * 60 * 1000) { // 5 minutes inactivity
                        Log.d("WidgetsBindingObserver", "Inactivity threshold met. Preserved flight log found.")
                        onStateResumed(savedState)
                    } else {
                        Log.d("WidgetsBindingObserver", "Saved state exists but threshold not met (${elapsedMs / 1000}s elapsed)")
                    }
                } else {
                    Log.d("WidgetsBindingObserver", "No saved state found in Firestore.")
                }
            } catch (e: Exception) {
                Log.e("WidgetsBindingObserver", "Error checking state: ${e.message}")
            }
        }
    }
}
