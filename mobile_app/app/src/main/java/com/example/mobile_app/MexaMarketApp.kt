package com.example.mobile_app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.example.mobile_app.data.local.AppDatabase
import com.example.mobile_app.data.remote.RealtimeEventService
import com.example.mobile_app.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MexaMarketApp : Application(), SingletonImageLoader.Factory {

    private var realtimeEventService: RealtimeEventService? = null

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        purgeCatalogCacheIfNeeded()
        startRealtimeListenerDelayed()
    }

    /**
     * Katalog Room keshi narx/ma'lumot o'zgarishlaridan keyin eskirib qolishi mumkin
     * (masalan narx qo'shilganda "—" ko'rinadi). Kesh versiyasi o'zgarganda butun
     * katalog keshi bir marta tozalanadi — keyingi ochilishda serverdan yangi ma'lumot
     * yuklanadi.
     */
    private fun purgeCatalogCacheIfNeeded() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val cached = prefs.getInt("catalog_cache_version", 0)
        if (cached != CATALOG_CACHE_VERSION) {
            prefs.edit().putInt("catalog_cache_version", CATALOG_CACHE_VERSION).apply()
            appScope.launch {
                try {
                    AppDatabase.get(this@MexaMarketApp).catalogProductDao().clearAll()
                } catch (e: Exception) {
                    android.util.Log.w("MexaMarketApp", "Catalog cache purge failed: ${e.message}")
                }
            }
        }
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return CoilConfig.imageLoader(context)
    }

    /**
     * Real-time SSE listener ni kechiktirib ishga tushiradi.
     * Dastur ochilgandan 30 soniya keyin ulanadi — bu dasturning tezroq yuklanishini ta'minlaydi
     * va past darajadagi qurilmalarda boshlang'ich xotira bosimini kamaytiradi.
     */
    private fun startRealtimeListenerDelayed() {
        appScope.launch {
            delay(30_000L) // 30 soniya kutish
            realtimeEventService = RealtimeEventService(
                baseUrl = RetrofitClient.BASE_URL,
                onEvent = { eventType, data ->
                    handleRealtimeEvent(eventType, data)
                }
            )
            realtimeEventService?.start()
        }
    }

    /**
     * Real-time eventlarni qayta ishlaydi.
     */
    private fun handleRealtimeEvent(eventType: String, data: String) {
        try {
            val json = org.json.JSONObject(data)
            when (eventType) {
                "PRODUCT_DELETED" -> {
                    val productId = json.optString("productId")
                    if (productId.isNotBlank()) {
                        ProductCacheManager.remove(productId)
                        sendBroadcast(android.content.Intent(ACTION_PRODUCT_DELETED).apply {
                            putExtra("productId", productId)
                            setPackage(packageName)
                        })
                    }
                }
                "PRODUCT_UPDATED" -> {
                    val productId = json.optString("productId")
                    if (productId.isNotBlank()) {
                        ProductCacheManager.remove(productId)
                        sendBroadcast(android.content.Intent(ACTION_PRODUCT_UPDATED).apply {
                            putExtra("productId", productId)
                            setPackage(packageName)
                        })
                    }
                }
                "PRODUCT_CREATED" -> {
                    sendBroadcast(android.content.Intent(ACTION_PRODUCT_CREATED).apply {
                        setPackage(packageName)
                    })
                }
                "ORDER_CREATED", "ORDER_STATUS_CHANGED" -> {
                    val eventUserId = json.optString("userId")
                    val myUserId = com.example.mobile_app.data.local.TokenManager.userId
                    if (eventUserId.isNotBlank() && eventUserId == myUserId) {
                        sendBroadcast(android.content.Intent(ACTION_ORDER_CHANGED).apply {
                            putExtra("orderId", json.optString("orderId"))
                            putExtra("orderStatus", json.optString("orderStatus"))
                            setPackage(packageName)
                        })
                    }
                }
                "NOTIFICATION_CREATED" -> {
                    val eventUserId = json.optString("userId")
                    val myUserId = com.example.mobile_app.data.local.TokenManager.userId
                    if (eventUserId.isNotBlank() && eventUserId == myUserId) {
                        sendBroadcast(android.content.Intent(ACTION_NOTIFICATION_ADDED).apply {
                            putExtra("notificationId", json.optString("notificationId"))
                            setPackage(packageName)
                        })
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MexaMarketApp", "Realtime event parse error: ${e.message}")
        }
    }

    override fun onTerminate() {
        realtimeEventService?.stop()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: MexaMarketApp
            private set

        const val ACTION_PRODUCT_DELETED = "com.example.mobile_app.PRODUCT_DELETED"
        const val ACTION_PRODUCT_UPDATED = "com.example.mobile_app.PRODUCT_UPDATED"
        const val ACTION_PRODUCT_CREATED = "com.example.mobile_app.PRODUCT_CREATED"
        const val ACTION_ORDER_CHANGED = "com.example.mobile_app.ORDER_CHANGED"
        const val ACTION_NOTIFICATION_ADDED = "com.example.mobile_app.NOTIFICATION_ADDED"

        /** Katalog Room kesh versiyasi — oshirilsa eski kesh bir marta tozalanadi. */
        const val CATALOG_CACHE_VERSION = 2
    }
}

/**
 * Mahsulot cache'ini boshqarish — real-time event kelganda cache'dan o'chirish.
 */
object ProductCacheManager {
    private val removedProducts = ConcurrentHashMap.newKeySet<String>()

    fun remove(productId: String) {
        removedProducts.add(productId)
    }

    fun isRemoved(productId: String): Boolean {
        return productId in removedProducts
    }

    fun clear() {
        removedProducts.clear()
    }
}
