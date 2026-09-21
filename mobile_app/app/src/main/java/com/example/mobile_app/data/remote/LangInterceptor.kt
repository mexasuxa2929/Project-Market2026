package com.example.mobile_app.data.remote

import com.example.mobile_app.util.AppLanguage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Barcha API so'rovlariga joriy til kodini (lang=uz|ru) qo'shadi.
 * Backend shu asosda lokalizatsiyalangan javob qaytaradi (product-service:
 * list/detail/batch; shop-service: cart va boshqalar) — UI'da per-item
 * localize() qilish va katta translation map'lar yuklash kerak bo'lmaydi.
 */
class LangInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
            .addQueryParameter("lang", AppLanguage.current.code)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
