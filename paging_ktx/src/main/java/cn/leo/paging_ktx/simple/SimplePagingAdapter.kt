package cn.leo.paging_ktx.simple

import android.view.View
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import cn.leo.paging_ktx.adapter.AdapterInterface
import cn.leo.paging_ktx.adapter.DifferData
import cn.leo.paging_ktx.adapter.ItemHelper
import cn.leo.paging_ktx.adapter.PagingAdapter
import cn.leo.paging_ktx.ext.getSuperClassGenericType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author : leo
 * @date : 2020/11/10
 * @description : 简易RvAdapter
 */
@Suppress("UNUSED", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
open class SimplePagingAdapter(
    vararg holders: SimpleHolder<*>,
) : PagingAdapter<DifferData>(
    itemCallback(
        areItemsTheSame = { old, new ->
            old.areItemsTheSame(new)
        },
        areContentsTheSame = { old, new ->
            old.areContentsTheSame(new)
        },
        getChangePayload = { old, new ->
            old.getChangePayload(new)
        }
    )
) {

    override var onItemClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit =
        AdapterInterface.DefaultOnItemClickListener
    override var onItemLongClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean =
        AdapterInterface.DefaultOnItemLongClickListener
    override var onItemChildClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit =
        AdapterInterface.DefaultOnItemChildClickListener
    override var onItemChildLongClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean =
        AdapterInterface.DefaultOnItemChildLongClickListener


    private val holderMap =
        mutableMapOf<Class<DifferData>, SimpleHolder<DifferData>?>()

    private var _recyclerView: RecyclerView? = null

    val recyclerView: RecyclerView
        get() {
            checkNotNull(_recyclerView) { "Please get it after onAttachedToRecyclerView()" }
            return _recyclerView!!
        }

    init {
        cacheHolder(holders)
        addOnPagesUpdatedListener {
            shouldComputePosition = true
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this._recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this._recyclerView = null
    }

    override fun getViewTypePosition(position: Int): Int {
        val holder = getHolder(getData(position)) ?: return 0
        computePositionAndItemCount()
        return position - holder.firstPosition
    }

    override fun getViewTypeCount(position: Int): Int {
        val holder = getHolder(getData(position)) ?: return itemCount
        computePositionAndItemCount()
        return holder.itemCount
    }

    private fun computePositionAndItemCount() {
        if (!shouldComputePosition) return
        shouldComputePosition = false
        holderMap.forEach { (clazz, holder) ->
            val position = getItems().indexOfFirst { it.javaClass == clazz }
            holder?.itemCount = getItems().count { it.javaClass == clazz }
            holder?.firstPosition = if (position == -1) 0 else position
        }
    }

    fun addHolder(holder: SimpleHolder<*>) {
        val key = holder::class.java.getSuperClassGenericType<DifferData>()
        val value = holder as? SimpleHolder<DifferData>
        holderMap[key] = value
    }

    private fun cacheHolder(holders: Array<out SimpleHolder<*>>) {
        holders.forEach { addHolder(it) }
    }

    protected fun setHolder(key: Class<DifferData>, holder: SimpleHolder<DifferData>) {
        holderMap[key] = holder
    }

    fun <T : DifferData> setList(scope: CoroutineScope, list: List<T>) {
        super.setPagingData(scope, PagingData.from(list))
    }

    fun <T : DifferData> setData(scope: CoroutineScope, pagingData: PagingData<T>) {
        super.setPagingData(scope, pagingData as PagingData<DifferData>)
    }

    fun <T : DifferData> setPager(pager: SimplePager<*, T>) {
        pager.getScope().launch {
            pager.getData().collectLatest {
                setData(this, it)
            }
        }
    }

    override fun isFullSpan(position: Int): Boolean {
        val holder = getHolder(getData(position))
        return holder?.isFullSpan(position) ?: false
    }

    private fun getHolder(data: DifferData?): SimpleHolder<DifferData>? {
        val key = if (data == null) {
            DifferData::class.java
        } else {
            data::class.java
        }
        return holderMap[key]
    }

    override fun getItemLayout(position: Int): Int {
        return try {
            //没有对应数据类型的holder
            val holder = getHolder(getData(position))
                ?: throw RuntimeException("SimplePagingAdapter : position=$position no match holder")
            holder.getItemLayout(position)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }

    }

    override fun bindData(item: ItemHelper, data: DifferData?, payloads: MutableList<Any>?) {
        val holder = getHolder(data) ?: return
        item.setItemHolder(holder, payloads)
    }
}