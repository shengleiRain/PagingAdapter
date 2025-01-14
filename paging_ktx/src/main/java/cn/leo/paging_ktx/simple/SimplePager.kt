package cn.leo.paging_ktx.simple

import androidx.paging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * @author : leo
 * @date : 2020/11/10
 * @description : 简易数据分页
 */
@OptIn(ExperimentalPagingApi::class)
class SimplePager<K : Any, V : Any> constructor(
    private val scope: CoroutineScope,
    private val pageSize: Int = 20,
    private val initialLoadSize: Int = pageSize,
    private val prefetchDistance: Int = pageSize,
    private val maxSize: Int = PagingConfig.MAX_SIZE_UNBOUNDED,
    private val enablePlaceholders: Boolean = false,
    private val initialKey: K? = null,
    private val pagingSource: () -> PagingSource<K, V>? = { null },
    private val remoteMediator: RemoteMediator<K, V>? = null,
    private val loadData:
    suspend (PagingSource.LoadParams<K>) -> PagingSource.LoadResult<K, V>? = { null }
) {

    fun getData(): Flow<PagingData<V>> {
        return Pager(
            PagingConfig(
                pageSize,
                initialLoadSize = initialLoadSize,
                prefetchDistance = prefetchDistance,
                maxSize = maxSize,
                enablePlaceholders = enablePlaceholders
            ),
            initialKey = initialKey,
            remoteMediator = remoteMediator
        ) {
            pagingSource() ?: object : PagingSource<K, V>() {
                override suspend fun load(params: LoadParams<K>): LoadResult<K, V> {
                    return loadData(params) ?: throw IllegalArgumentException(
                        "one of pagingSource or loadData must not null"
                    )
                }

                override fun getRefreshKey(state: PagingState<K, V>): K? {
                    return initialKey
                }
            }
        }.flow.cachedIn(scope)
    }

    fun getScope() = scope
}