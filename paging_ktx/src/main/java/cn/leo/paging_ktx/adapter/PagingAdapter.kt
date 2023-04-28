package cn.leo.paging_ktx.adapter

import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @author : leo
 * @date : 2020/5/11
 */
@Suppress("UNUSED", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
abstract class PagingAdapter<T : Any> : PagingDataAdapter<T, RecyclerView.ViewHolder>,
    AdapterInterface<T> {

    protected var shouldComputePosition = true

    constructor() : super(itemCallback())

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)

    companion object {
        fun <T> itemCallback(
            areItemsTheSame: (T, T) -> Boolean = { o, n -> o == n },
            areContentsTheSame: (T, T) -> Boolean = { o, n -> o == n },
            getChangePayload: (T, T) -> Any? = { _, _ -> null }
        ): DiffUtil.ItemCallback<T> {
            return object : DiffUtil.ItemCallback<T>() {
                override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                    return areItemsTheSame(oldItem, newItem)
                }

                override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
                    return areContentsTheSame(oldItem, newItem)
                }

                override fun getChangePayload(oldItem: T, newItem: T): Any? {
                    return getChangePayload(oldItem, newItem)
                }
            }
        }
    }

    //<editor-fold desc="子类必须实现">
    /**
     * 获取条目类型的布局
     *
     * @param position 索引
     * @return 布局id
     */
    @LayoutRes
    abstract override fun getItemLayout(position: Int): Int

    abstract override fun bindData(item: ItemHelper, data: T?, payloads: MutableList<Any>?)


    //</editor-fold>

    //<editor-fold desc="父类方法实现">
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SimpleViewHolder(
            parent,
            viewType,
            this as AdapterInterface<Any>,
            onItemClickListener,
            onItemLongClickListener,
            onItemChildClickListener,
            onItemChildLongClickListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? SimpleViewHolder)?.onBindViewHolder(position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItemLayout(position)
    }

    override fun getData(position: Int): T? {
        if (position < 0 || position >= itemCount) return null
        return try {
            getItem(position)
        } catch (e: Exception) {
            null
        }
    }

    override fun getItems(): List<T> {
        return snapshot().items
    }
    //</editor-fold>

    //<editor-fold desc="数据处理">
    /**
     * 保存提交的数据集
     */
    protected var mPagingData: PagingData<T> = PagingData.empty()
        set(value) {
            field = value
            submitPagingData()
            shouldComputePosition = true
        }

    /**
     * 协程
     */
    protected lateinit var mScope: CoroutineScope

    /**
     * 采用setPagingData 可以动态增减数据
     */
    open fun setPagingData(scope: CoroutineScope, pagingData: PagingData<T>) {
        mScope = scope
        mPagingData = pagingData
    }

    /**
     * 提交数据
     */
    private fun submitPagingData() {
        mScope.launch {
            submitData(mPagingData)
        }
    }

    /**
     * 向尾部添加数据
     */
    override fun appendItem(item: T) {
        if (!this::mScope.isInitialized) {
            throw IllegalArgumentException("To add data, you must use the 'setPagingData' method")
        }
        mPagingData = mPagingData.insertFooterItem(item = item)
    }

    /**
     * 向首部添加数据
     */
    override fun prependItem(item: T) {
        if (!this::mScope.isInitialized) {
            throw IllegalArgumentException("To add data, you must use the 'setPagingData' method")
        }
        mPagingData = mPagingData.insertHeaderItem(item = item)
    }

    /**
     * 过滤数据
     * @param predicate 条件为false的移除，为true的保留
     */
    fun filterItem(predicate: suspend (T) -> Boolean) {
        if (!this::mScope.isInitialized) {
            throw IllegalArgumentException("To edit data, you must use the 'setPagingData' method")
        }
        mPagingData = mPagingData.filter(predicate)
    }

    /**
     * 移除数据
     * @param item 要移除的条目
     */
    override fun removeItem(item: T) {
        filterItem { it != item }
    }

    /**
     * 移除数据
     * @param position 要移除的条目的索引
     */
    fun removeItem(position: Int) {
        filterItem { it != getData(position) }
    }

    /**
     * 修改条目内容
     * @param position 条目索引
     * @param payload 局部刷新
     */
    override fun edit(@IntRange(from = 0) position: Int, payload: Any?, block: (T?) -> Unit) {
        if (position >= itemCount) {
            return
        }
        block(getData(position))
        notifyItemChanged(position, payload)
    }

    //</editor-fold>
    /**
     * 局部刷新
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val viewHolder = holder as? SimpleViewHolder
            val helper = viewHolder?.itemHelper
            val itemHolder = helper?.mItemHolder
            val item = getItem(position)
            if (itemHolder != null) {
                itemHolder.bindData(helper, item, payloads)
            } else {
                viewHolder?.onBindViewHolder(position, payloads)
            }
        }
    }


    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        val viewHolder = holder as? SimpleViewHolder
        val helper = viewHolder?.itemHelper
        val itemHolder = helper?.mItemHolder
        itemHolder?.onViewDetach(helper)
    }

    //<editor-fold desc="状态监听">

    /**
     * 刷新状态监听
     */
    private val mOnRefreshStateListenerArray
            by lazy { ArrayList<(State) -> Unit>() }

    /**
     * 向后加载更多状态监听
     */
    private val mOnLoadMoreStateListenerArray
            by lazy { ArrayList<(State) -> Unit>() }

    /**
     * 向前加载更多监听
     */
    private val mOnPrependStateListenerArray
            by lazy { ArrayList<(State) -> Unit>() }

    init {
        addLoadStateListener {
            dispatchState(
                it.refresh,
                it.source.append.endOfPaginationReached,
                mOnRefreshStateListenerArray
            )
            dispatchState(
                it.append,
                it.source.append.endOfPaginationReached,
                mOnLoadMoreStateListenerArray
            )
            dispatchState(
                it.prepend,
                it.source.append.endOfPaginationReached,
                mOnPrependStateListenerArray
            )
        }
    }

    private fun dispatchState(
        loadState: LoadState,
        noMoreData: Boolean,
        stateListener: ArrayList<(State) -> Unit>
    ) {
        when (loadState) {
            is LoadState.Loading -> {
                observer(State.Loading, stateListener)
            }

            is LoadState.NotLoading -> {
                observer(State.Success(noMoreData), stateListener)
            }

            is LoadState.Error -> {
                observer(State.Error, stateListener)
            }
        }
    }

    /**
     * 通知给所有订阅者
     */
    private fun observer(state: State, stateListener: ArrayList<(State) -> Unit>) {
        stateListener.forEach { it(state) }
    }

    /**
     * 刷新状态监听
     */
    fun addOnRefreshStateListener(listener: (State) -> Unit) {
        mOnRefreshStateListenerArray += listener
    }

    /**
     * 向后加载更多状态监听
     */
    fun addOnLoadMoreStateListener(listener: (State) -> Unit) {
        mOnLoadMoreStateListenerArray += listener
    }

    /**
     * 向前加载更多状态监听
     */
    fun addOnPrependStateListener(listener: (State) -> Unit) {
        mOnPrependStateListenerArray += listener
    }

    //</editor-fold>
}