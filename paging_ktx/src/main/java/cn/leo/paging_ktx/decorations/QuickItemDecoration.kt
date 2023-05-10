package cn.leo.paging_ktx.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.leo.paging_ktx.adapter.AdapterInterface
import cn.leo.paging_ktx.adapter.DecorationPadding
import cn.leo.paging_ktx.adapter.SimpleViewHolder
import cn.leo.paging_ktx.simple.SimpleHolder

/*********************************************************************
 * Created by shenglei on 2023/4/27.
 *********************************************************************/
class QuickItemDecoration(
    private val decorationPadding: DecorationPadding = DecorationPadding()
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager ?: return
        val viewHolder = parent.getChildViewHolder(view)
        var position = viewHolder.bindingAdapterPosition
        val bindingAdapter = viewHolder.bindingAdapter

        var paddings = decorationPadding
        if (bindingAdapter is AdapterInterface<*>) {
            position = bindingAdapter.getViewTypePosition(position)
            val itemHolder =
                (viewHolder as? SimpleViewHolder)?.itemHelper?.mItemHolder as? SimpleHolder
            paddings = itemHolder?.decorationPadding ?: decorationPadding
        }

        when (layoutManager) {
            is GridLayoutManager -> {
                val spanCount = layoutManager.spanCount
                val lp = view.layoutParams as GridLayoutManager.LayoutParams

                val sizeAvg = (paddings.hSide * 2 + paddings.hSpace * (spanCount - 1)) / spanCount
                val spanSize = lp.spanSize
                val spanIndex = lp.spanIndex
                outRect.left =
                    computeLeft(spanIndex, sizeAvg, spanCount, paddings.hSide, paddings.hSpace)
//                if (spanSize == 0 || spanSize == spanCount) {
//                    outRect.right = sizeAvg - outRect.left
//                } else {
//
//                }

                outRect.right = computeRight(
                    spanIndex + spanSize - 1,
                    sizeAvg,
                    spanCount,
                    paddings.hSide,
                    paddings.hSpace
                )

                if (position * spanSize < spanCount) { // top edge
                    outRect.top = paddings.vSide
                }
                outRect.bottom = paddings.vSpace // item bottom

            }

            is LinearLayoutManager -> {
                val orientation = layoutManager.orientation
                if (orientation == LinearLayoutManager.VERTICAL) {
                    verticalPadding(outRect, paddings, position)
                } else {
                    horizontalPadding(outRect, paddings, position)
                }
            }

        }
    }

    private fun computeLeft(
        spanIndex: Int,
        sizeAvg: Int,
        spanCount: Int,
        hSide: Int,
        hSpace: Int
    ): Int {
        return if (spanIndex == 0) {
            hSide
        } else if (spanIndex >= spanCount / 2) {
            //从右边算起
            sizeAvg - computeRight(spanIndex, sizeAvg, spanCount, hSide, hSpace)
        } else {
            //从左边算起
            hSpace - computeRight(spanIndex - 1, sizeAvg, spanCount, hSide, hSpace)
        }
    }

    private fun computeRight(
        spanIndex: Int,
        sizeAvg: Int,
        spanCount: Int,
        hSide: Int,
        hSpace: Int
    ): Int {
        return if (spanIndex == spanCount - 1) {
            hSide
        } else if (spanIndex >= spanCount / 2) {
            //从右边算起
            hSpace - computeLeft(spanIndex + 1, sizeAvg, spanCount, hSide, hSpace)
        } else {
            //从左边算起
            sizeAvg - computeLeft(spanIndex, sizeAvg, spanCount, hSide, hSpace)
        }
    }

    private fun horizontalPadding(
        outRect: Rect,
        paddings: DecorationPadding,
        position: Int
    ) {
        outRect.top = paddings.vSide
        outRect.bottom = paddings.vSide
        if (position == 0) {
            outRect.left = paddings.hSide
        }
        outRect.right = paddings.hSpace
    }

    private fun verticalPadding(
        outRect: Rect,
        paddings: DecorationPadding,
        position: Int
    ) {
        outRect.left = paddings.hSide
        outRect.right = paddings.hSide
        if (position == 0) {
            outRect.top = paddings.vSide
        }
        outRect.bottom = paddings.vSpace
    }
}