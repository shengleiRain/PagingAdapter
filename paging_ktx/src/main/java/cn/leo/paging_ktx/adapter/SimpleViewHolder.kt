package cn.leo.paging_ktx.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/*********************************************************************
 * Created by shenglei on 2023/4/26.
 *********************************************************************/
class SimpleViewHolder internal constructor(
    parent: ViewGroup,
    layout: Int,
    private val adapter: AdapterInterface<Any>,
    private val mOnItemClickListener: (adapter: AdapterInterface<out Any>, view: View, position: Int) -> Unit = { _, _, _ -> },
    private val mOnItemLongClickListener: (adapter: AdapterInterface<out Any>, view: View, position: Int) -> Boolean = { _, _, _ -> false },
    private val mOnItemChildClickListenerProxy: (adapter: AdapterInterface<out Any>, view: View, position: Int) -> Unit = { _, _, _ -> },
    private val mOnItemChildLongClickListenerProxy: (adapter: AdapterInterface<out Any>, view: View, position: Int) -> Boolean = { _, _, _ -> false },
) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
    ),
    View.OnClickListener,
    View.OnLongClickListener {
    val itemHelper: ItemHelper = ItemHelper(this)

    init {
        itemHelper.setLayoutResId(layout)
        itemHelper.setOnItemChildClickListener(mOnItemChildClickListenerProxy)
        itemHelper.setOnItemChildLongClickListener(mOnItemChildLongClickListenerProxy)
        itemHelper.setRVAdapter(adapter)
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    val mPosition: Int
        get() = if (bindingAdapterPosition == -1) {
            bindPosition
        } else {
            bindingAdapterPosition
        }

    //传进来的position值，在手动创建holder时候bindingAdapterPosition为-1，需要使用传进来的值
    var bindPosition: Int = 0

    fun onBindViewHolder(position: Int, payloads: MutableList<Any>? = null) {
        bindPosition = position
        adapter.bindData(itemHelper, adapter.getData(position), payloads)
    }

    override fun onClick(v: View) {
        mOnItemClickListener(adapter, v, mPosition)
    }

    override fun onLongClick(v: View): Boolean {
        return mOnItemLongClickListener(adapter, v, mPosition)
    }
}