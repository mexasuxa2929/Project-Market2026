package com.example.mobile_app.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.mobile_app.data.local.CatalogProductDao
import com.example.mobile_app.data.local.CatalogProductEntity
import com.example.mobile_app.data.remote.ApiService
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Offline-first RemoteMediator: tarmoq javobi Room'ga yoziladi, PagingSource
 * esa har doim Room'dan o'qiydi. Tarmoq yo'q bo'lsa ham eski kesh ko'rinadi.
 * Cursor = oxirgi qaytarilgan mahsulot id (keyset pagination).
 */
@OptIn(ExperimentalPagingApi::class)
class CatalogRemoteMediator(
    private val apiService: ApiService,
    private val dao: CatalogProductDao,
    private val queryKey: String,
    private val search: String,
    private val categoryId: String?,
    private val sort: String?,
    private val gson: Gson
) : RemoteMediator<Int, CatalogProductEntity>() {

    /** Keshda saqlanadigan maksimal qidiruv so'rovlari soni (qolganlari o'chiriladi). */
    private val maxCachedQueries = 10

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CatalogProductEntity>
    ): MediatorResult {
        val cursor = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.APPEND -> state.lastItemOrNull()?.id
                ?: return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val response = apiService.getProducts(
                search = search.ifBlank { null },
                categoryId = categoryId,
                sort = sort,
                cursor = cursor,
                size = state.config.pageSize
            )

            if (loadType == LoadType.REFRESH) {
                dao.clearByQuery(queryKey)
                dao.deleteStaleKeys(queryKey, maxCachedQueries)
            }
            dao.upsertAll(
                response.content.map { CatalogProductEntity(queryKey, it.id, gson.toJson(it)) }
            )

            MediatorResult.Success(endOfPaginationReached = !response.hasMore)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}