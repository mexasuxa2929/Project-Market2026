package com.example.mobile_app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.mobile_app.data.local.CatalogProductDao
import com.example.mobile_app.data.model.Brand
import com.example.mobile_app.data.model.Category
import com.example.mobile_app.data.model.HomeBundleData
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.paging.CatalogRemoteMediator
import com.example.mobile_app.data.remote.ApiService
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.util.AppLanguage
import com.example.mobile_app.util.Language
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

data class ProductPage(
    val products: List<Product>,
    val hasMore: Boolean,
    val nextCursor: String?
)

private fun Product.localize(lang: Language): Product = copy(
    name = nameTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: name,
    description = descriptionTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: description,
    shortDescription = shortDescriptionTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: shortDescription,
    material = materialTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: material,
    countryOfOrigin = countryOfOriginTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: countryOfOrigin,
    manufacturerName = manufacturerNameTranslations?.get(lang.code)?.takeIf { it.isNotBlank() } ?: manufacturerName
)

class ProductRepository(
    private val apiService: ApiService,
    private val catalogProductDao: CatalogProductDao
) {

    private val gson = Gson()

    private fun Product.withAbsoluteUrls() = copy(
        imageUrls = imageUrls?.map { RetrofitClient.toAbsoluteUrl(it) ?: it }
    )

    fun getProducts(search: String? = null): Flow<Result<List<Product>>> = flow {
        try {
            val page = apiService.getProducts(search = search)
            val lang = AppLanguage.current
            emit(Result.success(page.content.map { it.withAbsoluteUrls().localize(lang) }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getTrendingProducts(): Flow<Result<List<Product>>> = flow {
        try {
            val page = apiService.getProducts(size = 10)
            val lang = AppLanguage.current
            emit(Result.success(page.content.map { it.withAbsoluteUrls().localize(lang) }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    /** Keyset pagination: cursor = oxirgi qaytarilgan mahsulot id, null = 1-sahifa */
    fun getProductsPage(cursor: String?, size: Int = 10, search: String? = null): Flow<Result<ProductPage>> = flow {
        try {
            val response = apiService.getProducts(search = search, cursor = cursor, size = size)
            val lang = AppLanguage.current
            val products = response.content.map { it.withAbsoluteUrls().localize(lang) }
            emit(Result.success(ProductPage(products, response.hasMore, response.nextCursor)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Homepage bundle (2-bosqich): GET api/shops/home — trending + recommended +
     * brands + categories bitta so'rovda. Xato bo'lsa caller eski 4-sorovli yo'lga qaytadi.
     */
    fun getHomeBundle(): Flow<Result<HomeBundleData>> = flow {
        try {
            val bundle = apiService.getHome().data
                ?: throw IllegalStateException("Bo'sh javob")
            val lang = AppLanguage.current
            emit(Result.success(bundle.copy(
                trending = bundle.trending.map { it.withAbsoluteUrls().localize(lang) },
                recommended = bundle.recommended.map {
                    it.copy(thumbnail = RetrofitClient.toAbsoluteUrl(it.thumbnail))
                },
                brands = bundle.brands.map {
                    it.copy(logoUrl = RetrofitClient.toAbsoluteUrl(it.logoUrl))
                },
                categories = bundle.categories.map {
                    it.copy(imageUrl = RetrofitClient.toAbsoluteUrl(it.imageUrl))
                }
            )))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    fun getProductById(id: String): Flow<Result<Product>> = flow {
        try {
            val lang = AppLanguage.current
            val product = apiService.getProductById(id).withAbsoluteUrls().localize(lang)
            emit(Result.success(product))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun searchProducts(query: String): Flow<Result<List<Product>>> = flow {
        try {
            val page = apiService.getProducts(search = query)
            val lang = AppLanguage.current
            emit(Result.success(page.content.map { it.withAbsoluteUrls().localize(lang) }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Paging 3 + Room (offline-first): RemoteMediator tarmoqdan cursor bo'yicha
     * o'qiydi va Room'ga yozadi; UI esa doim Room'dan yuklanadi.
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getProductsPager(search: String = "", categoryId: String? = null, sort: String? = null): Flow<PagingData<Product>> {
        val sortKey = sort ?: ""
        val catKey = categoryId ?: ""
        val queryKey = "${AppLanguage.current.code}:${search.trim().lowercase()}:${catKey}:${sortKey}"
        val lang = AppLanguage.current
        val rawSearch = search.trim()
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                prefetchDistance = 4,
                initialLoadSize = 10,
                enablePlaceholders = false
            ),
            remoteMediator = CatalogRemoteMediator(apiService, catalogProductDao, queryKey, rawSearch, categoryId, sort, gson)
        ) {
            catalogProductDao.pagingSource(queryKey)
        }
            .flow
            .map { pagingData ->
                pagingData.map { entity ->
                    gson.fromJson(entity.json, Product::class.java)
                        .withAbsoluteUrls()
                        .localize(lang)
                }
            }
    }

    fun getCategories(): Flow<Result<List<Category>>> = flow {
        try {
            val response = apiService.getCategories(active = true, size = 100)
            val items = (response.data?.content ?: emptyList()).map { category ->
                category.copy(imageUrl = RetrofitClient.toAbsoluteUrl(category.imageUrl))
            }
            emit(Result.success(items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearCatalogCache() {
        catalogProductDao.clearAll()
    }

    fun getBrands(): Flow<Result<List<Brand>>> = flow {
        try {
            val response = apiService.getBrands(active = true, size = 8)
            val items = (response.data?.content ?: emptyList()).map { brand ->
                brand.copy(logoUrl = RetrofitClient.toAbsoluteUrl(brand.logoUrl))
            }
            emit(Result.success(items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}