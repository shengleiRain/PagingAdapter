package cn.leo.paging_ktx.layoutmanager

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.leo.paging_ktx.adapter.AdapterInterface
import cn.leo.paging_ktx.adapter.FullSpanAdapterType
import cn.leo.paging_ktx.simple.SimplePagingAdapter

/*********************************************************************
 * Created by shenglei on 2023/4/26.
 *********************************************************************/
class QuickGridLayoutManager : GridLayoutManager {
    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, spanCount: Int) : super(context, spanCount)

    constructor(
        context: Context, spanCount: Int,
        @RecyclerView.Orientation orientation: Int, reverseLayout: Boolean
    ) : super(context, spanCount, orientation, reverseLayout)

    private val fullSpanSizeLookup = FullSpanSizeLookup()

    private var adapter: RecyclerView.Adapter<*>? = null

    init {
        fullSpanSizeLookup.originalSpanSizeLookup = spanSizeLookup
        super.setSpanSizeLookup(fullSpanSizeLookup)
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        adapter = newAdapter
    }

    override fun setSpanSizeLookup(spanSizeLookup: SpanSizeLookup?) {
        fullSpanSizeLookup.originalSpanSizeLookup = spanSizeLookup
    }

    private inner class FullSpanSizeLookup : SpanSizeLookup() {
        var originalSpanSizeLookup: SpanSizeLookup? = null

        override fun getSpanSize(position: Int): Int {
            val adapter = adapter ?: return 1
            var realAdapter = adapter
            var realPosition = position
            if (adapter is ConcatAdapter) {
                val pair = adapter.getWrappedAdapterAndPosition(position)
                realAdapter = pair.first
                realPosition = pair.second
            }
            return when (realAdapter) {
                is FullSpanAdapterType -> {
                    spanCount
                }

                is AdapterInterface<*> -> {
                    if (realAdapter.isFullSpan(realPosition)) {
                        spanCount
                    } else {
                        1
                    }
                }

                else -> originalSpanSizeLookup?.getSpanSize(realPosition) ?: 1
            }
        }
    }
}