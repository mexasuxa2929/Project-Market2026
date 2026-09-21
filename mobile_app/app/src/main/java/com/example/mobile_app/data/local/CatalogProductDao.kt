package com.example.mobile_app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CatalogProductEntity>)

    @Query("DELETE FROM catalog_products WHERE queryKey = :queryKey")
    suspend fun clearByQuery(queryKey: String)

    @Query("DELETE FROM catalog_products")
    suspend fun clearAll()

    /**
     * Eski qidiruv so'rovlarining keshini tozalaydi: joriy key'dan boshqa eng
     * so'nggi :keepCount ta key saqlanadi, qolganlari o'chiriladi (DB cheksiz o'smasligi uchun).
     */
    @Query("""
        DELETE FROM catalog_products
        WHERE queryKey != :currentKey
          AND queryKey IN (
            SELECT queryKey FROM (
              SELECT queryKey, MAX(rowid) AS lastUsed
              FROM catalog_products
              WHERE queryKey != :currentKey
              GROUP BY queryKey
              ORDER BY lastUsed DESC
              LIMIT -1 OFFSET :keepCount
            )
          )
    """)
    suspend fun deleteStaleKeys(currentKey: String, keepCount: Int)

    @Query("SELECT * FROM catalog_products WHERE queryKey = :queryKey ORDER BY id")
    fun pagingSource(queryKey: String): PagingSource<Int, CatalogProductEntity>
}