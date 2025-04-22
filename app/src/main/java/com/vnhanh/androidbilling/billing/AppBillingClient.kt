package com.vnhanh.androidbilling.billing

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResult
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.querySkuDetails

class AppBillingClient private constructor() : PurchasesUpdatedListener, BillingClientStateListener {
    private var billingClient: BillingClient? = null
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    
    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        billingClient?.startConnection(this)
    }

    override fun onBillingServiceDisconnected() {
        Log.d("TestAlan", "Billing service disconnected")
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d("TestAlan", "Billing service connected $billingResult")
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        purchasesUpdatedListener?.onProcessing()
        if ((billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    || billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            && purchases != null
        ) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED || purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) {
                            purchasesUpdatedListener?.onSuccess(purchase)
                        }
                    } else {
                        purchasesUpdatedListener?.onSuccess(purchase)
                    }
                } else {
                    purchasesUpdatedListener?.onFailure("Purchase failed. Please contact support for help")
                }
            }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                purchasesUpdatedListener?.onFailure("You're already subscribed. Please try Restore Purchases or contact support for help")
            } else {
                purchasesUpdatedListener?.onFailure("Purchase failed. Please contact support for help")
            }
        }
    }

    suspend fun purchase(sku: String, activity: FragmentActivity, callback: PurchasesUpdatedListener) {
        purchasesUpdatedListener = callback

        val params = SkuDetailsParams.newBuilder()
        val type = BillingClient.SkuType.SUBS
        params.setSkusList(listOf(sku)).setType(type)

        val skuDetails: SkuDetailsResult = billingClient?.querySkuDetails(params.build()) ?: return
        val (billingResult: BillingResult, detailsList: List<SkuDetails>?) = skuDetails

        Log.d("TestAlan", "skuDetails: $skuDetails")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !detailsList.isNullOrEmpty()) {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(detailsList.first())
                .build()
            billingClient?.launchBillingFlow(activity, flowParams)
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            purchasesUpdatedListener?.onFailure("Purchase failed. Please contact support for help")
        }
    }
    
    fun dispose() {
        billingClient?.endConnection()
        billingClient = null
        purchasesUpdatedListener = null
    }

    interface PurchasesUpdatedListener {
        fun onSuccess(purchase: Purchase)

        fun onFailure(message: String?)

        fun onProcessing() {}
    }
    
    companion object {
        private var singleObject: AppBillingClient? = null
        
        fun getInstance(): AppBillingClient {
            if (singleObject == null) {
                singleObject = AppBillingClient()
            }
            return singleObject!!
        }
    }
}
