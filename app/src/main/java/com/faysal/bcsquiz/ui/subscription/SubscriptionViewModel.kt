package com.faysal.bcsquiz.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.faysal.bcsquiz.data.repository.BillingRepository
import com.android.billingclient.api.BillingClient

class SubscriptionViewModel(private val billingRepository: BillingRepository) : ViewModel() {

    fun purchasePro(activity: Activity) {
        billingRepository.launchBillingFlow(activity, "pro_membership", BillingClient.ProductType.SUBS)
    }

    fun buyPoints(activity: Activity) {
        billingRepository.launchBillingFlow(activity, "points_500", BillingClient.ProductType.INAPP)
    }
}
