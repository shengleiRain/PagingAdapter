package cn.leo.paging_ktx.ext

import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import cn.leo.paging_ktx.adapter.DifferData
import cn.leo.paging_ktx.decorations.FloatDecoration
import cn.leo.paging_ktx.simple.SimpleCheckedAdapter
import cn.leo.paging_ktx.simple.SimpleHolder
import cn.leo.paging_ktx.simple.SimplePager
import cn.leo.paging_ktx.simple.SimplePagingAdapter

/**
 * @author : ling luo
 * @date : 2022/3/24
 * @description : PagingAdapter dsl拓展
 */

//作用域进行限制
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class ClickDsl

interface BaseDslAdapterBuilder {
    fun setLayoutManager(layoutManager: RecyclerView.LayoutManager)
    fun addItemDecoration(itemDecoration: ItemDecoration)
}

interface DslConcatAdapterBuilder : BaseDslAdapterBuilder {
    val adapter: ConcatAdapter
    fun <T : RecyclerView.Adapter<*>> addAdapter(adapter: T): T

    fun <T : RecyclerView.Adapter<*>> addAdapter(dsl: (RecyclerView.() -> T)): T
}

interface DslAdapterBuilder : BaseDslAdapterBuilder {
    fun <T : DifferData> setPager(pager: SimplePager<*, T>)
}

interface DslSimpleAdapterBuilder : DslAdapterBuilder {
    val adapter: SimplePagingAdapter
    fun <T : DifferData> addHolder(
        holder: SimpleHolder<T>,
        isFloatItem: Boolean = false,
        dsl: (@ClickDsl DslClickBuilder<T>.() -> Unit)? = null
    )
}

interface DslSimpleCheckedAdapterBuilder : DslAdapterBuilder {
    val adapter: SimpleCheckedAdapter
    fun <T : DifferData> addHolder(
        holder: SimpleHolder<T>,
        isClickChecked: Boolean = true,
        isFloatItem: Boolean = false,
        dsl: (@ClickDsl DslCheckedBuilder<T>.() -> Unit)? = null
    )
}

interface DslClickBuilder<T : DifferData> {
    fun onItemClick(onClick: OnItemClick<T>)
    fun onItemLongClick(onClick: OnItemClick<T>)
    fun onItemChildClick(@IdRes viewId: Int, onClick: OnItemClick<T>)
    fun onItemChildLongClick(@IdRes viewId: Int, onClick: OnItemClick<T>)
}

interface DslCheckedBuilder<T : DifferData> : DslClickBuilder<T> {
    fun onItemChecked(onChecked: OnItemChecked)
    fun setChecked(position: Int, isChecked: Boolean): Boolean
    fun getCheckedPositionList(): List<Int>
    fun getCheckedItemList(): List<T>
}

data class ItemInfo<T : DifferData>(
    val data: T,
    val position: Int,
    val view: View,
    val adapter: SimplePagingAdapter,
    val recyclerView: RecyclerView,
)

/**
 * 条目选择拓展属性
 */
var <T : DifferData> ItemInfo<T>.isChecked: Boolean
    get() {
        if (adapter is SimpleCheckedAdapter) {
            return adapter.itemIsChecked(position)
        }
        return false
    }
    set(value) {
        if (adapter is SimpleCheckedAdapter) {
            adapter.setChecked(position, value)
        }
    }

data class CheckedInfo(
    val position: Int,
    val isChecked: Boolean,
    val isAllChecked: Boolean,
    val checkedCount: Int,
    val allCanCheckedCount: Int,
    val adapter: SimplePagingAdapter,
    val recyclerView: RecyclerView,
)

fun interface OnItemClick<T : DifferData> {
    fun onClick(itemInfo: ItemInfo<T>)
}

fun interface OnItemChecked {
    fun onChecked(checkedInfo: CheckedInfo)
}

@Suppress("UNCHECKED_CAST")
open class DslClickBuilderImpl<T : DifferData>(
    private val holder: SimpleHolder<T>
) : DslClickBuilder<T> {
    protected val clazz = holder.getDataClassType()

    private var onItemClickListener: OnItemClick<T>? = null
    private var onItemLongClickListener: OnItemClick<T>? = null
    private var onItemChildClickListener = mutableMapOf<Int, OnItemClick<T>>()
    private var onItemChildLongClickListener = mutableMapOf<Int, OnItemClick<T>>()

    open fun doItemClick(
        position: Int,
        v: View,
        adapter: SimplePagingAdapter,
        recyclerView: RecyclerView
    ) {
        val item = adapter.getData(position)
        if (item != null && item::class.java == clazz) {
            val itemInfo = ItemInfo(item as T, position, v, adapter, recyclerView)
            onItemClickListener?.onClick(itemInfo)
        }
    }

    fun doItemLongClick(
        position: Int,
        v: View,
        adapter: SimplePagingAdapter,
        recyclerView: RecyclerView
    ) {
        val item = adapter.getData(position)
        if (item != null && item::class.java == clazz) {
            val itemInfo = ItemInfo(item as T, position, v, adapter, recyclerView)
            onItemLongClickListener?.onClick(itemInfo)
        }
    }

    fun doItemChildClick(
        position: Int,
        v: View,
        adapter: SimplePagingAdapter,
        recyclerView: RecyclerView
    ) {
        val item = adapter.getData(position)
        if (item != null && item::class.java == clazz) {
            val itemInfo = ItemInfo(item as T, position, v, adapter, recyclerView)
            onItemChildClickListener[v.id]?.onClick(itemInfo)
        }
    }

    fun doItemChildLongClick(
        position: Int,
        v: View,
        adapter: SimplePagingAdapter,
        recyclerView: RecyclerView
    ) {
        val item = adapter.getData(position)
        if (item != null && item::class.java == clazz) {
            val itemInfo = ItemInfo(item as T, position, v, adapter, recyclerView)
            onItemChildLongClickListener[v.id]?.onClick(itemInfo)
        }
    }

    override fun onItemClick(onClick: OnItemClick<T>) {
        onItemClickListener = onClick
    }

    override fun onItemLongClick(onClick: OnItemClick<T>) {
        onItemLongClickListener = onClick
    }

    override fun onItemChildClick(@IdRes viewId: Int, onClick: OnItemClick<T>) {
        holder.itemChildClickIds.add(viewId)
        onItemChildClickListener[viewId] = onClick
    }

    override fun onItemChildLongClick(@IdRes viewId: Int, onClick: OnItemClick<T>) {
        holder.itemChildLongClickIds.add(viewId)
        onItemChildLongClickListener[viewId] = onClick
    }

}

class DslCheckedBuilderImpl<T : DifferData>(
    private val adapter: SimpleCheckedAdapter,
    private val isClickChecked: Boolean,
    holder: SimpleHolder<T>
) : DslClickBuilderImpl<T>(holder), DslCheckedBuilder<T> {

    internal var onCheckedCallback: OnItemChecked? = null

    override fun doItemClick(
        position: Int,
        v: View,
        adapter: SimplePagingAdapter,
        recyclerView: RecyclerView
    ) {
        super.doItemClick(position, v, adapter, recyclerView)
        if (!isClickChecked) return
        val item = adapter.getData(position)
        if (item != null && item::class.java == clazz) {
            if (adapter is SimpleCheckedAdapter) {
                adapter.setChecked(position, !adapter.itemIsChecked(position))
            }
        }
    }

    override fun onItemChecked(onChecked: OnItemChecked) {
        onCheckedCallback = onChecked
    }

    override fun setChecked(position: Int, isChecked: Boolean): Boolean {
        return adapter.setChecked(position, isChecked)
    }

    override fun getCheckedPositionList(): List<Int> {
        return adapter.getCheckedPositionList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCheckedItemList(): List<T> {
        return adapter.getCheckedPositionList()
            .map { adapter.getData(it) as T }
    }
}

class ClickEventStore(val recyclerView: RecyclerView, val adapter: SimplePagingAdapter) {

    private val clickEventList = mutableListOf<DslClickBuilderImpl<*>>()

    fun addClickEvent(clickBuilder: DslClickBuilderImpl<*>) {
        clickEventList.add(clickBuilder)
    }

    init {
        adapter.onItemClickListener = { _, v, position ->
            clickEventList.forEach {
                it.doItemClick(position, v, adapter, recyclerView)
            }
        }
        adapter.onItemLongClickListener = { _, v, position ->
            clickEventList.forEach {
                it.doItemLongClick(position, v, adapter, recyclerView)
            }
            true
        }
        adapter.onItemChildClickListener = { _, v, position ->
            clickEventList.forEach {
                it.doItemChildClick(position, v, adapter, recyclerView)
            }
        }
        adapter.onItemChildLongClickListener = { _, v, position ->
            clickEventList.forEach {
                it.doItemChildLongClick(position, v, adapter, recyclerView)
            }
            true
        }
    }
}

class DslSimpleAdapterImpl(val recyclerView: RecyclerView) : DslSimpleAdapterBuilder {
    private val simplePagingAdapter = SimplePagingAdapter()
    internal var mLayoutManager: RecyclerView.LayoutManager =
        LinearLayoutManager(recyclerView.context)

    private val clickEventStore = ClickEventStore(recyclerView, adapter)

    override val adapter: SimplePagingAdapter
        get() = simplePagingAdapter

    override fun <T : DifferData> addHolder(
        holder: SimpleHolder<T>,
        isFloatItem: Boolean,
        dsl: (@ClickDsl DslClickBuilder<T>.() -> Unit)?
    ) {
        val clickBuilder = DslClickBuilderImpl(holder)
        clickEventStore.addClickEvent(clickBuilder)
        if (dsl != null) {
            clickBuilder.dsl()
        }
        if (isFloatItem) {
            recyclerView.addItemDecoration(FloatDecoration(holder.getItemLayout()))
        }
        adapter.addHolder(holder)
    }

    override fun setLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        mLayoutManager = layoutManager
    }

    override fun addItemDecoration(itemDecoration: ItemDecoration) {
        recyclerView.addItemDecoration(itemDecoration)
    }

    override fun <T : DifferData> setPager(pager: SimplePager<*, T>) {
        adapter.setPager(pager)
    }
}

class DslSimpleCheckedAdapterImpl(val recyclerView: RecyclerView) : DslSimpleCheckedAdapterBuilder {
    private val simpleCheckedAdapter = SimpleCheckedAdapter()
    internal var mLayoutManager: RecyclerView.LayoutManager =
        LinearLayoutManager(recyclerView.context)

    private val clickEventStore = ClickEventStore(recyclerView, adapter)

    private val checkedEventList = mutableListOf<OnItemChecked>()

    override val adapter: SimpleCheckedAdapter
        get() = simpleCheckedAdapter

    init {
        adapter.setOnCheckedCallback { position, isChecked,
                                       isAllChecked, checkedCount,
                                       allCanCheckedCount ->
            val checkedInfo = CheckedInfo(
                position, isChecked, isAllChecked,
                checkedCount, allCanCheckedCount,
                adapter, recyclerView
            )
            checkedEventList.forEach {
                it.onChecked(checkedInfo)
            }
        }
    }


    override fun <T : DifferData> addHolder(
        holder: SimpleHolder<T>,
        isClickChecked: Boolean,
        isFloatItem: Boolean,
        dsl: (@ClickDsl DslCheckedBuilder<T>.() -> Unit)?
    ) {
        val clickBuilder = DslCheckedBuilderImpl(
            adapter,
            isClickChecked,
            holder
        )
        clickEventStore.addClickEvent(clickBuilder)
        if (dsl != null) {
            clickBuilder.dsl()
        }
        if (isFloatItem) {
            recyclerView.addItemDecoration(FloatDecoration(holder.getItemLayout()))
        }
        val onCheckedCallback = clickBuilder.onCheckedCallback
        if (onCheckedCallback != null) {
            checkedEventList.add(onCheckedCallback)
        }
        adapter.addHolder(holder)
    }

    override fun setLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        mLayoutManager = layoutManager
    }

    override fun addItemDecoration(itemDecoration: ItemDecoration) {
        recyclerView.addItemDecoration(itemDecoration)
    }

    override fun <T : DifferData> setPager(pager: SimplePager<*, T>) {
        adapter.setPager(pager)
    }
}

class DslConcatAdapterBuilderImpl(
    val recyclerView: RecyclerView,
    config: ConcatAdapter.Config = ConcatAdapter.Config.DEFAULT
) : DslConcatAdapterBuilder {
    private val concatAdapter = ConcatAdapter(config)
    override val adapter: ConcatAdapter
        get() = concatAdapter

    internal var mLayoutManager: RecyclerView.LayoutManager =
        LinearLayoutManager(recyclerView.context)

    override fun <T : RecyclerView.Adapter<*>> addAdapter(adapter: T): T {
        concatAdapter.addAdapter(adapter)
        return adapter
    }

    override fun <T : RecyclerView.Adapter<*>> addAdapter(dsl: (RecyclerView.() -> T)): T {
        val adapter = dsl(recyclerView)
        return addAdapter(adapter)
    }

    override fun setLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        mLayoutManager = layoutManager
    }

    override fun addItemDecoration(itemDecoration: ItemDecoration) {
        recyclerView.addItemDecoration(itemDecoration)
    }

}

/**
 * 构建简易列表适配器
 */
@Suppress("UNUSED")
fun RecyclerView.buildAdapter(init: @ClickDsl DslSimpleAdapterBuilder.() -> Unit): SimplePagingAdapter {
    val dslSimpleAdapterImpl = DslSimpleAdapterImpl(this)
    dslSimpleAdapterImpl.init()
    layoutManager = dslSimpleAdapterImpl.mLayoutManager
    adapter = dslSimpleAdapterImpl.adapter
    return dslSimpleAdapterImpl.adapter
}

/**
 * 构建简易选择列表适配器
 */
@Suppress("UNUSED")
fun RecyclerView.buildCheckedAdapter(init: @ClickDsl DslSimpleCheckedAdapterBuilder.() -> Unit): SimpleCheckedAdapter {
    val dslSimpleAdapterImpl = DslSimpleCheckedAdapterImpl(this)
    dslSimpleAdapterImpl.init()
    layoutManager = dslSimpleAdapterImpl.mLayoutManager
    adapter = dslSimpleAdapterImpl.adapter
    return dslSimpleAdapterImpl.adapter
}

fun RecyclerView.buildConcatAdapter(
    config: ConcatAdapter.Config = ConcatAdapter.Config.DEFAULT,
    init: DslConcatAdapterBuilder.() -> Unit
): ConcatAdapter {
    val impl = DslConcatAdapterBuilderImpl(this, config)
    impl.init()
    layoutManager = impl.mLayoutManager
    adapter = impl.adapter
    return impl.adapter
}