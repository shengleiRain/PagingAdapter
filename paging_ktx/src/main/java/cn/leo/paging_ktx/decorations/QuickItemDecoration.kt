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
open class QuickItemDecoration(
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
        var itemCount = layoutManager.itemCount
        if (bindingAdapter is AdapterInterface<*>) {
            position = bindingAdapter.getViewTypePosition(position)
            itemCount = bindingAdapter.getViewTypeCount(position)
            val itemHolder =
                (viewHolder as? SimpleViewHolder)?.itemHelper?.mItemHolder as? SimpleHolder
            paddings = itemHolder?.decorationPadding ?: decorationPadding
        }

        when (layoutManager) {
            is GridLayoutManager -> {
                val spanCount = layoutManager.spanCount
                val lp = view.layoutParams as GridLayoutManager.LayoutParams

                val sizeAvg =
                    (paddings.leftSide + paddings.rightSide + paddings.hSpace * (spanCount - 1)) / spanCount
                val spanSize = lp.spanSize
                val spanIndex = lp.spanIndex
                outRect.left =
                    computeLeft(
                        spanIndex,
                        sizeAvg,
                        spanCount,
                        paddings.leftSide,
                        paddings.rightSide,
                        paddings.hSpace
                    )

                outRect.right = computeRight(
                    spanIndex + spanSize - 1,
                    sizeAvg,
                    spanCount,
                    paddings.leftSide,
                    paddings.rightSide,
                    paddings.hSpace
                )

                if (position * spanSize < spanCount) { // top edge
                    outRect.top = paddings.topSide
                }
                outRect.bottom = paddings.vSpace // item bottom
            }

            is LinearLayoutManager -> {
                val orientation = layoutManager.orientation
                if (orientation == LinearLayoutManager.VERTICAL) {
                    verticalPadding(outRect, paddings, position, itemCount)
                } else {
                    horizontalPadding(outRect, paddings, position, itemCount)
                }
            }

        }
    }

    private fun computeLeft(
        spanIndex: Int,
        sizeAvg: Int,
        spanCount: Int,
        leftSide: Int,
        rightSide: Int,
        hSpace: Int
    ): Int {
        return if (spanIndex == 0) {
            leftSide
        } else if (spanIndex >= spanCount / 2) {
            //从右边算起
            sizeAvg - computeRight(spanIndex, sizeAvg, spanCount, leftSide, rightSide, hSpace)
        } else {
            //从左边算起
            hSpace - computeRight(spanIndex - 1, sizeAvg, spanCount, leftSide, rightSide, hSpace)
        }
    }

    private fun computeRight(
        spanIndex: Int,
        sizeAvg: Int,
        spanCount: Int,
        leftSide: Int,
        rightSide: Int,
        hSpace: Int
    ): Int {
        return if (spanIndex == spanCount - 1) {
            rightSide
        } else if (spanIndex >= spanCount / 2) {
            //从右边算起
            hSpace - computeLeft(spanIndex + 1, sizeAvg, spanCount, leftSide, rightSide, hSpace)
        } else {
            //从左边算起
            sizeAvg - computeLeft(spanIndex, sizeAvg, spanCount, leftSide, rightSide, hSpace)
        }
    }

    private fun horizontalPadding(
        outRect: Rect,
        paddings: DecorationPadding,
        position: Int,
        itemCount: Int,
    ) {
        outRect.top = paddings.topSide
        outRect.bottom = paddings.bottomSide
        when (position) {
            0 -> {
                outRect.left = paddings.leftSide
            }

            itemCount - 1 -> {
                outRect.right = paddings.rightSide
            }

            else -> {
                outRect.right = paddings.hSpace
            }
        }
    }

    private fun verticalPadding(
        outRect: Rect,
        paddings: DecorationPadding,
        position: Int,
        itemCount: Int,
    ) {
        outRect.left = paddings.leftSide
        outRect.right = paddings.rightSide
        when (position) {
            0 -> {
                outRect.top = paddings.topSide
            }

            itemCount - 1 -> {
                outRect.bottom = paddings.bottomSide
            }

            else -> {
                outRect.bottom = paddings.vSpace
            }
        }
    }

    protected fun isFirstRaw(manager: GridLayoutManager, pos: Int, childCount: Int): Boolean {
        if (childCount <= 0) {
            return false
        }
        val lookup: GridLayoutManager.SpanSizeLookup = manager.spanSizeLookup
        val spanCount = manager.spanCount
        return lookup.getSpanGroupIndex(pos, spanCount) == lookup.getSpanGroupIndex(0, spanCount)
    }

    protected fun isLastRaw(manager: GridLayoutManager, pos: Int, childCount: Int): Boolean {
        if (childCount <= 0) {
            return false
        }
        val lookup: GridLayoutManager.SpanSizeLookup = manager.spanSizeLookup
        val spanCount = manager.spanCount
        return lookup.getSpanGroupIndex(
            pos,
            spanCount
        ) == lookup.getSpanGroupIndex(childCount - 1, spanCount)
    }

}