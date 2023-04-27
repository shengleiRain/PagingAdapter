package cn.leo.paging_ktx.simple

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import cn.leo.paging_ktx.adapter.DecorationPadding
import cn.leo.paging_ktx.adapter.DifferData
import cn.leo.paging_ktx.adapter.ItemHelper
import cn.leo.paging_ktx.adapter.ItemHolder
import cn.leo.paging_ktx.ext.getSuperClassGenericType

/**
 * @author : leo
 * @date : 2020/11/10
 * @description : 简易holder
 */
abstract class SimpleHolder<T : DifferData>(
    @LayoutRes val res: Int = 0,
    val decorationPadding: DecorationPadding = DecorationPadding()
) :
    ItemHolder<T>() {
    /**
     * 该类型Holder第一次出现的地方，便于后续计算该类型ViewHolder的位置
     */
    var firstPosition = 0

    @LayoutRes
    open fun getItemLayout(position: Int = -1): Int = res

    /**
     * 是否占满一行
     */
    open fun isFullSpan(position: Int = -1): Boolean = false

    fun getDataClassType() = this::class.java.getSuperClassGenericType<T>()

    /**
     * 子view点击id列表
     */
    internal val itemChildClickIds = hashSetOf<Int>()

    /**
     * 子view长按id列表
     */
    internal val itemChildLongClickIds = hashSetOf<Int>()


    final override fun bindData(
        item: ItemHelper,
        data: T?,
        payloads: MutableList<Any>?
    ) {
        if (data == null) return //简易holder data不为空
        itemChildClickIds.forEach(item::addOnClickListener)
        itemChildLongClickIds.forEach(item::addOnLongClickListener)
        bindItem(item, data, payloads)
    }

    abstract fun bindItem(
        item: ItemHelper,
        data: T,
        payloads: MutableList<Any>?
    )
}