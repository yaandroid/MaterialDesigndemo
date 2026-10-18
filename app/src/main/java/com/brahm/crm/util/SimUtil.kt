package com.brahm.crm.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimOption(val subId: Int, val label: String)

object SimUtil {

    /** Returns selectable SIMs; always includes a "Default SIM" (-1) option first. */
    fun listSims(context: Context): List<SimOption> {
        val out = mutableListOf(SimOption(-1, "Default SIM"))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return out
        }
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return out
            @Suppress("MissingPermission")
            val list = sm.activeSubscriptionInfoList ?: emptyList()
            list.forEach { info ->
                val name = info.displayName?.toString()?.ifBlank { null }
                    ?: info.carrierName?.toString()?.ifBlank { null }
                    ?: "SIM ${info.simSlotIndex + 1}"
                out.add(SimOption(info.subscriptionId, "$name (SIM ${info.simSlotIndex + 1})"))
            }
        } catch (t: Throwable) {
            // ignore — return whatever we have
        }
        return out
    }
}
