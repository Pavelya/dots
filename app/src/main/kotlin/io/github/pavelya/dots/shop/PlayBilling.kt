package io.github.pavelya.dots.shop

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * The real Google Play Billing (Play Billing Library), for one-time products only. Each call waits
 * for Google Play's answer; the billing library calls back on the main thread.
 */
class PlayBilling(context: Context) : BillingGateway {
    @Volatile
    private var listener: (Int, List<PlayPurchase>) -> Unit = { _, _ -> }

    /** The products the last [prices] found, which a Buy needs. */
    @Volatile
    private var details: Map<String, ProductDetails> = emptyMap()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> listener(result.responseCode, purchases.orEmpty().map(::playPurchase)) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun listen(listener: (code: Int, purchases: List<PlayPurchase>) -> Unit) {
        this.listener = listener
    }

    override suspend fun connect(): Int {
        if (client.isReady) return BillingResponseCode.OK
        return answer { done ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) = done(result.responseCode)

                override fun onBillingServiceDisconnected() = done(BillingResponseCode.SERVICE_DISCONNECTED)
            })
        }
    }

    override suspend fun country(): String? = answer { done ->
        client.getBillingConfigAsync(GetBillingConfigParams.newBuilder().build()) { result, config ->
            done(config?.countryCode?.takeIf { result.responseCode == BillingResponseCode.OK })
        }
    }

    override suspend fun prices(productIds: List<String>): Answer<Map<String, String>> = answer { done ->
        val products = productIds.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, found ->
            val list = found.productDetailsList
            if (result.responseCode == BillingResponseCode.OK) details = list.associateBy { it.productId }
            val prices = list.mapNotNull { product -> product.oneTimePurchaseOfferDetails?.let { product.productId to it.formattedPrice } }.toMap()
            done(Answer(result.responseCode, prices))
        }
    }

    override suspend fun purchases(): Answer<List<PlayPurchase>> = answer { done ->
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            done(Answer(result.responseCode, purchases.map(::playPurchase)))
        }
    }

    override suspend fun acknowledge(token: String): Int = answer { done ->
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) { result -> done(result.responseCode) }
    }

    override fun launch(activity: Activity, productId: String): Int {
        val product = details[productId] ?: return BillingResponseCode.ITEM_UNAVAILABLE
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        product.oneTimePurchaseOfferDetails?.offerToken?.let(params::setOfferToken)
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build()
        return client.launchBillingFlow(activity, flow).responseCode
    }

    private fun playPurchase(purchase: Purchase) = PlayPurchase(
        products = purchase.products,
        paid = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
        pending = purchase.purchaseState == Purchase.PurchaseState.PENDING,
        token = purchase.purchaseToken,
        acknowledged = purchase.isAcknowledged,
    )

    /** Waits for the first answer a callback gives; the billing library may call a listener more than once. */
    private suspend fun <T> answer(ask: (done: (T) -> Unit) -> Unit): T = suspendCancellableCoroutine { waiting ->
        val once = AtomicBoolean(false)
        ask { value -> if (once.compareAndSet(false, true) && waiting.isActive) waiting.resume(value) }
    }
}
