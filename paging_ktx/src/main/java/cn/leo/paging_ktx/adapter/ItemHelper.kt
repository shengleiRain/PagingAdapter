package cn.leo.paging_ktx.adapter

/*********************************************************************
 * Created by shenglei on 2023/4/26.
 *********************************************************************/
import android.content.Context
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.core.app.ActivityCompat

@Suppress("UNUSED", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
class ItemHelper(private val viewHolder: SimpleViewHolder) :
    View.OnClickListener, View.OnLongClickListener {

    private val viewCache = SparseArray<View>()
    private val clickListenerCache by lazy { ArrayList<Int>() }
    private val longClickListenerCache by lazy { ArrayList<Int>() }
    private val mTags by lazy { HashMap<String, Any>() }
    lateinit var adapter: AdapterInterface<Any> private set

    @LayoutRes
    @get:LayoutRes
    var itemLayoutResId: Int = 0
    val position get() = viewHolder.mPosition
    val itemView get() = viewHolder.itemView
    val context: Context = itemView.context
    var tag: Any? = null

    /**
     * the relative position of the view type in the adapter
     */
    val viewTypePosition: Int get() = adapter.getViewTypePosition(position)

    private lateinit var mOnItemChildClickListener:
                (adapter: AdapterInterface<Any>, v: View, position: Int) -> Unit

    private lateinit var mOnItemChildLongClickListener:
                (adapter: AdapterInterface<Any>, v: View, position: Int) -> Boolean

    fun setLayoutResId(@LayoutRes layoutResId: Int) {
        this.itemLayoutResId = layoutResId
    }

    fun setOnItemChildClickListener(
        onItemChildClickListener:
            (adapter: AdapterInterface<Any>, v: View, position: Int) -> Unit
    ) {
        mOnItemChildClickListener = onItemChildClickListener
    }

    fun setOnItemChildLongClickListener(
        onItemChildLongClickListener: (
            adapter: AdapterInterface<out Any>,
            v: View, position: Int
        ) -> Boolean
    ) {
        mOnItemChildLongClickListener = onItemChildLongClickListener
    }

    fun setRVAdapter(pagedListAdapter: AdapterInterface<Any>) {
        adapter = pagedListAdapter
    }

    fun setTag(key: String, tag: Any) {
        mTags[key] = tag
    }

    fun getTag(key: String): Any? {
        return mTags[key]
    }

    fun bind(block: View.() -> Unit): ItemHelper {
        block(itemView)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : View> findViewById(@IdRes viewId: Int): V {
        val v = viewCache.get(viewId)
        val view: V?
        if (v == null) {
            view = itemView.findViewById(viewId)
            if (view == null) {
                val entryName = itemView.resources.getResourceEntryName(viewId)
                throw NullPointerException("id: R.id.$entryName can not find in this item!")
            }
            viewCache.put(viewId, view)
        } else {
            view = v as V
        }
        return view
    }

    fun <V : View> getViewById(@IdRes viewId: Int, block: (V) -> Unit = {}): ItemHelper {
        try {
            block(findViewById(viewId))
        } catch (e: Exception) {
            Log.e("ItemHelper", "getViewById: ", e)
        }
        return this
    }

    /**
     * 给按钮或文本框设置文字
     *
     * @param viewId 控件id
     * @param text   设置的文字
     */
    fun setText(
        @IdRes viewId: Int,
        text: CharSequence?,
        block: TextView.() -> Unit = {}
    ): ItemHelper {
        getViewById<View>(viewId) {
            if (it is TextView) {
                it.text = text
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not TextView")
            }
        }
        return this
    }

    /**
     * 给按钮或文本框设置文字
     *
     * @param viewId 控件id
     * @param resId  设置的文字资源
     */
    fun setText(
        @IdRes viewId: Int,
        @StringRes resId: Int,
        block: TextView.() -> Unit = {}
    ): ItemHelper {
        getViewById<View>(viewId) {
            if (it is TextView) {
                it.text = try {
                    it.resources.getString(resId)
                } catch (e: Exception) {
                    resId.toString()
                }
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not TextView")
            }
        }
        return this
    }

    /**
     * 设置文本颜色
     *
     * @param viewId 要设置文本的控件，TextView及其子类都可以
     * @param color  颜色int值，不是资源Id
     */
    fun setTextColor(
        @IdRes viewId: Int,
        @ColorInt color: Int,
        block: TextView.() -> Unit = {}
    ): ItemHelper {
        getViewById<View>(viewId) {
            if (it is TextView) {
                it.setTextColor(color)
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not TextView")
            }
        }
        return this
    }

    /**
     * 设置文本颜色
     *
     * @param viewId     要设置文本的控件，TextView及其子类都可以
     * @param colorResId 颜色资源Id
     */
    fun setTextColorRes(
        @IdRes viewId: Int,
        @ColorRes colorResId: Int,
        block: TextView.() -> Unit = {}
    ): ItemHelper {
        getViewById<View>(viewId) {
            if (it is TextView) {
                it.setTextColor(ActivityCompat.getColor(context, colorResId))
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not TextView")
            }
        }
        return this
    }

    /**
     * 给图片控件设置资源图片
     *
     * @param viewId 图片控件id
     * @param resId  资源id
     */
    fun setImageResource(
        @IdRes viewId: Int,
        @DrawableRes resId: Int,
        block: ImageView.() -> Unit
    ): ItemHelper {
        getViewById<View>(viewId) {
            if (it is ImageView) {
                it.setImageResource(resId)
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not ImageView")
            }
        }
        return this
    }

    /**
     * 给图片控件设置图片
     * @param viewId 图片控件id
     */
    fun setImage(@IdRes viewId: Int, block: ImageView.() -> Unit): ItemHelper {
        getViewById<View>(viewId) {
            if (it is ImageView) {
                block(it)
            } else {
                val entryName = it.resources.getResourceEntryName(viewId)
                throw ClassCastException("id: R.id.$entryName are not ImageView")
            }
        }
        return this
    }

    /**
     * 设置view的背景
     *
     * @param viewId 控件id
     * @param resId  资源id
     */
    fun setBackgroundResource(@IdRes viewId: Int, @DrawableRes resId: Int): ItemHelper {
        getViewById<View>(viewId) {
            it.setBackgroundResource(resId)
        }
        return this
    }

    /**
     * 设置view的背景颜色
     *
     * @param viewId 控件id
     * @param color  颜色值，不是资源值
     */
    fun setBackgroundColor(@IdRes viewId: Int, @ColorInt color: Int): ItemHelper {
        getViewById<View>(viewId) {
            it.setBackgroundColor(color)
        }
        return this
    }

    fun setVisibility(@IdRes viewId: Int, visibility: Int): ItemHelper {
        getViewById<View>(viewId) {
            it.visibility = visibility
        }
        return this
    }

    fun setVisibleOrGone(@IdRes viewId: Int, visibility: () -> Boolean): ItemHelper {
        getViewById<View>(viewId) {
            it.visibility = if (visibility()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        return this
    }

    fun setVisibleOrInVisible(@IdRes viewId: Int, visibility: () -> Boolean): ItemHelper {
        getViewById<View>(viewId) {
            it.visibility = if (visibility()) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }
        return this
    }

    fun setViewVisible(@IdRes vararg viewId: Int): ItemHelper {
        for (id in viewId) {
            getViewById<View>(id) {
                it.visibility = View.VISIBLE
            }
        }
        return this
    }

    fun setViewInvisible(@IdRes vararg viewId: Int): ItemHelper {
        for (id in viewId) {
            getViewById<View>(id) {
                it.visibility = View.INVISIBLE
            }
        }
        return this
    }

    fun setViewGone(@IdRes vararg viewId: Int): ItemHelper {
        for (id in viewId) {
            getViewById<View>(id) {
                it.visibility = View.GONE
            }
        }
        return this
    }

    /**
     * 给条目中的view添加点击事件
     *
     * @param viewId 控件id
     */
    fun addOnClickListener(@IdRes viewId: Int): ItemHelper {
        val contains = clickListenerCache.contains(viewId)
        if (!contains) {
            getViewById<View>(viewId) { it.setOnClickListener(this) }
            clickListenerCache.add(viewId)
        }
        return this
    }

    /**
     * 给条目中的view添加点击事件
     *
     * @param view 控件
     */
    fun addOnClickListener(view: View): ItemHelper {
        addOnClickListener(view.id)
        return this
    }

    /**
     * 给条目中的view添加长按事件
     *
     * @param viewId 控件id
     */
    fun addOnLongClickListener(@IdRes viewId: Int): ItemHelper {
        val contains = longClickListenerCache.contains(viewId)
        if (!contains) {
            getViewById<View>(viewId) { it.setOnLongClickListener(this) }
            longClickListenerCache.add(viewId)
        }
        return this
    }

    /**
     * 给条目中的view添加长按事件
     *
     * @param view 控件
     */
    fun addOnLongClickListener(view: View): ItemHelper {
        addOnLongClickListener(view.id)
        return this
    }

    override fun onClick(v: View) {
        if (::mOnItemChildClickListener.isInitialized) {
            mOnItemChildClickListener(adapter, v, position)
        }
    }


    override fun onLongClick(v: View): Boolean {
        if (::mOnItemChildLongClickListener.isInitialized) {
            mOnItemChildLongClickListener(adapter, v, position)
            return true
        }
        return false
    }

    var mItemHolder: ItemHolder<Any>? = null

    fun setItemHolder(
        itemHolder: ItemHolder<out Any>,
        payloads: MutableList<Any>? = null
    ): ItemHolder<Any>? {
        try {
            if (mItemHolder == null) {
                mItemHolder = itemHolder as? ItemHolder<Any>?
                mItemHolder?.initView(this, adapter.getData(position))
            }
            mItemHolder?.bindData(this, adapter.getData(position), payloads)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return mItemHolder
    }

}