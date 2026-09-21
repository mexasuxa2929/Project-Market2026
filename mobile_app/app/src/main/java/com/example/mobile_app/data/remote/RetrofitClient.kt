package com.example.mobile_app.data.remote

import android.content.Context
import android.os.Build
import com.example.mobile_app.MexaMarketApp
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Lokal (kompyuter-server): gateway orqali — Emulator: 10.0.2.2 | Real qurilma: WiFi IP
    const val BASE_URL = "http://192.168.1.9:8080/"

    private val context: Context get() = MexaMarketApp.instance

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.example.mobile_app.util.DeviceUtils.isDebuggable(context)) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
    }

    /** 25MB HTTP kesh — mahsulot/kategoriya/brend ro'yxatlari qayta kirishda tarmoqdan emas, keshdan olinadi */
    private val httpCache = Cache(context.cacheDir, 25L * 1024 * 1024)

    /**
     * Homepage ro'yxatlari uchun Cache-Control: backend kesh header yubormagani uchun
     * shu yerda qo'shiladi. Faqat xavfsiz GET ro'yxatlar (savat/buyurtma/wishlist ga tegilmaydi).
     * OkHttp `public` bo'lgani uchun Authorization header bo'lsa ham keshlaydi.
     */
    private val productListCacheInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.method != "GET") return@Interceptor response
        val path = request.url.encodedPath
        val maxAge = when {
            path == "/api/products" -> 60          // trending ro'yxat
            path == "/api/categories" -> 300       // kam o'zgaradi
            path == "/api/brands" -> 300           // kam o'zgaradi
            path.contains("recommended") -> 60     // recommended ro'yxat
            else -> null
        }
        if (maxAge == null) return@Interceptor response
        response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAge")
            .removeHeader("Pragma")
            .build()
    }

    /** Offline bo'lsa ham keshdan ma'lumot olish */
    private val offlineCacheControl = CacheControl.Builder()
        .maxStale(7, TimeUnit.DAYS)
        .build()

    /** Kam parallel ulanishlar — past darajadagi qurilmalarda xotirani tejaydi */
    private val okHttpDispatcher = Dispatcher().apply {
        maxRequests = 8
        maxRequestsPerHost = 4
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dispatcher(okHttpDispatcher)
        .cache(httpCache)
        .addInterceptor(AuthInterceptor())
        .addInterceptor(LangInterceptor())
        // Keshga yozish uchun javob header ini to'g'rilaydi (faqat tarmoq javoblariga)
        .addNetworkInterceptor(productListCacheInterceptor)
        .authenticator(TokenAuthenticator())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val cartApiService: CartApiService = retrofit.create(CartApiService::class.java)
    val orderApiService: OrderApiService = retrofit.create(OrderApiService::class.java)
    val reviewApiService: ReviewApiService = retrofit.create(ReviewApiService::class.java)
    val wishlistApiService: WishlistApiService = retrofit.create(WishlistApiService::class.java)
    val notificationApiService: NotificationApiService = retrofit.create(NotificationApiService::class.java)
    val addressApiService: AddressApiService = retrofit.create(AddressApiService::class.java)

    fun toAbsoluteUrl(path: String?): String? {
        if (path == null) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = BASE_URL.trimEnd('/')
        val clean = path.trimStart('/')
        return "$base/$clean"
    }
}
