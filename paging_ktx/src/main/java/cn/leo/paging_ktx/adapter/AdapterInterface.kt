package cn.leo.paging_ktx.adapter

import android.view.View
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes

/*********************************************************************
 * Created by shenglei on 2023/4/26.
 *********************************************************************/
interface AdapterInterface<T : Any> {
    companion object {
        val DefaultOnItemClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit =
            { _, _, _ -> }
        val DefaultOnItemLongClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean =
            { _, _, _ -> false }
        val DefaultOnItemChildClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit =
            { _, _, _ -> }
        val DefaultOnItemChildLongClickListener: (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean =
            { _, _, _ -> false }
    }

    var onItemClickListener:
                (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit

    var onItemLongClickListener:
                (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean

    var onItemChildClickListener:
                (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Unit

    var onItemChildLongClickListener:
                (adapter: AdapterInterface<out Any>, v: View, position: Int) -> Boolean

    /**
     * 获取条目类型的布局
     *
     * @param position 索引
     * @return 布局id
     */
    @LayoutRes
    fun getItemLayout(position: Int): Int

    /**
     * 给条目绑定数据
     *
     * @param item  条目帮助类
     * @param data    对应数据
     * @param payloads item局部变更
     */
    fun bindData(
        item: ItemHelper,
        data: T?,
        payloads: MutableList<Any>? = null
    )

    fun getItems(): List<T>

    fun getData(position: Int): T?

    fun appendItem(item: T)

    fun prependItem(item: T)

    fun removeItem(item: T)

    fun getViewTypePosition(position: Int): Int = position

    fun edit(@IntRange(from = 0) position: Int, payload: Any? = null, block: (T?) -> Unit = {})
}