package cn.leo.paging_ktx.simple

import androidx.annotation.IntRange
import androidx.paging.PagingData
import cn.leo.paging_ktx.adapter.CheckedData
import cn.leo.paging_ktx.adapter.DifferData
import kotlinx.coroutines.CoroutineScope

/**
 * @author : ling luo
 * @date : 2022/3/30
 * @description : 可选择的 adapter
 *
 * 需要解决分页加载 和全选冲突问题
 *
 */
@Suppress("UNUSED")
open class SimpleCheckedAdapter : SimplePagingAdapter() {
    enum class CheckedModel {
        NONE, SINGLE, MULTI  //选择模式，无，单选/多选
    }

    private var checkedModel = CheckedModel.NONE  //当前选择模式

    private var singleCheckIndex = -1 //单选索引

    private var singleModelCancelable = false  //单选是否可取消选择

    private var multiCheckIndexList: MutableSet<Int> = mutableSetOf() //多选索引

    private var maxCheckedNum = Int.MAX_VALUE  //多选限制，最多选择多少个

    private var onCheckedCallback: OnCheckedCallback? = null //多选回调

    private var onMaxSelectCallback: OnMaxSelectCallback? = null //超过最多选择回调

    fun interface OnCheckedCallback {
        fun onChecked(
            position: Int,
            isChecked: Boolean,
            isAllChecked: Boolean,
            checkedCount: Int, //分页加载会变化
            allCanCheckedCount: Int //分页加载会变化
        )
    }

    /**
     * 超过最多选择
     */
    fun interface OnMaxSelectCallback {
        fun onSelectOverMax(max: Int)
    }

    /**
     * 设置选择回调
     */
    fun setOnCheckedCallback(callback: OnCheckedCallback) {
        onCheckedCallback = callback
    }

    /**
     * 更新所有条目
     */
    private fun notifyAllItem() {
        notifyItemRangeChanged(
            0,
            itemCount,
            checkedModel
        )
    }

    /**
     * 设置选择模式
     */
    private fun setCheckModel(model: CheckedModel) {
        checkedModel = model
        notifyAllItem()
    }

    /**
     * 关闭选择模式
     */
    open fun closeCheckModel() {
        setCheckModel(CheckedModel.NONE)
    }

    /**
     * 设置单选模式
     * @param cancelable 是否可取消选择
     */
    open fun setSingleCheckModel(cancelable: Boolean = true) {
        singleModelCancelable = cancelable
        if (!cancelable && singleCheckIndex == -1) {
            singleCheckIndex = 0
        }
        setCheckModel(CheckedModel.SINGLE)
    }

    /**
     * 设置多选模式
     */
    open fun setMultiCheckModel() {
        setCheckModel(CheckedModel.MULTI)
    }

    /**
     * 是否是单选模式
     */
    open fun isSingleCheckedModel(): Boolean {
        return checkedModel == CheckedModel.SINGLE
    }

    /**
     * 是否是多选模式
     */
    open fun isMultiCheckedModel(): Boolean {
        return checkedModel == CheckedModel.MULTI
    }

    /**
     * 设置最大选择数
     * @param max 最大选择数
     * @param onMaxSelectCallback 超过最大选择回调
     */
    open fun setMaxChecked(
        @IntRange(from = 1L) max: Int,
        onMaxSelectCallback: OnMaxSelectCallback? = null
    ) {
        this.maxCheckedNum = max
        this.onMaxSelectCallback = onMaxSelectCallback
        if (multiCheckIndexList.size > max) {
            cancelChecked()
        }
    }

    /**
     * 反选
     */
    open fun reverseChecked() {
        if (checkedModel != CheckedModel.MULTI) return
        //获取所有是CheckedData的数据
        val left = (0 until itemCount)
            .asSequence()
            .filter { getData(it) is CheckedData }
            .toMutableSet()
        left.removeAll(multiCheckIndexList)
        val copy = multiCheckIndexList.toMutableSet()
        copy.forEach {
            setChecked(it, false)
        }
        for (i in left) {
            if (!setChecked(i, true)) {
                return
            }
        }
    }

    /**
     * 全选
     */
    open fun checkedAll() {
        if (checkedModel != CheckedModel.MULTI) return
        val all = (0 until itemCount).filter {
            getData(it) is CheckedData
        }.toMutableSet()
        //排除已选中的条目，剩余全部设置选中
        val left = all.toMutableSet()
        left.removeAll(multiCheckIndexList)
        for (i in left) {
            if (!setChecked(i, true)) {
                return
            }
        }
    }

    /**
     * 取消选择
     */
    open fun cancelChecked() {
        singleCheckIndex = -1
        val copy = multiCheckIndexList.toMutableSet()
        copy.forEach {
            setChecked(it, false)
        }
    }

    /**
     * 设置条目选择状态
     * @param position 条目索引
     * @param isChecked 是否选择
     * @return 是否改变状态
     */
    open fun setChecked(position: Int, isChecked: Boolean): Boolean {
        //非可选择条目，不可选择
        if (getData(position) !is CheckedData) return false
        when (checkedModel) {
            CheckedModel.SINGLE -> {
                if (!singleModelCancelable && !isChecked) return false
                if (singleCheckIndex == position && isChecked) return false
                if (singleCheckIndex != -1) {
                    notifyItemChanged(singleCheckIndex)
                }
                singleCheckIndex = if (isChecked) position else -1
                notifyItemChanged(position)
                onCheckedCallback?.onChecked(
                    position,
                    isChecked,
                    isAllChecked(),
                    if (singleCheckIndex == -1) 0 else 1,
                    canCheckedItemCount()
                )
                return true
            }

            CheckedModel.MULTI -> {
                val contains = multiCheckIndexList.contains(position)
                if (contains == isChecked) return false
                if (isChecked) {
                    return if (multiCheckIndexList.size < maxCheckedNum) {
                        multiCheckIndexList.add(position)
                        notifyItemChanged(position, isChecked)
                        onCheckedCallback?.onChecked(
                            position,
                            isChecked,
                            isAllChecked(),
                            multiCheckIndexList.size,
                            canCheckedItemCount()
                        )
                        true
                    } else {
                        //超过最大选择数
                        onMaxSelectCallback?.onSelectOverMax(maxCheckedNum)
                        false
                    }
                } else {
                    multiCheckIndexList.remove(position)
                    notifyItemChanged(position, isChecked)
                    onCheckedCallback?.onChecked(
                        position,
                        isChecked,
                        isAllChecked(),
                        multiCheckIndexList.size,
                        canCheckedItemCount()
                    )
                    return true
                }
            }

            else -> {
                return false
            }
        }
    }

    /**
     * 判断是否全选
     */
    open fun isAllChecked(): Boolean {
        if (checkedModel == CheckedModel.SINGLE) {
            return canCheckedItemCount() == 1 && singleCheckIndex != -1
        }
        return multiCheckIndexList.size == canCheckedItemCount()
    }

    /**
     * 返回可选择条目总数
     */
    private fun canCheckedItemCount(): Int {
        return snapshot().count { it is CheckedData }
    }


    /**
     * 判断条目是否被选中
     */
    open fun itemIsChecked(position: Int): Boolean {
        return when (checkedModel) {
            CheckedModel.MULTI -> multiCheckIndexList.contains(position)
            CheckedModel.SINGLE -> singleCheckIndex == position
            CheckedModel.NONE -> false
        }
    }

    /**
     * 获取所有已选择的索引
     */
    open fun getCheckedPositionList(): List<Int> {
        if (isSingleCheckedModel()) {
            return if (singleCheckIndex == -1) {
                emptyList()
            } else {
                listOf(singleCheckIndex)
            }
        }
        return multiCheckIndexList.toList()
    }

    /**
     * 获取已选择数量
     */
    open fun getCheckedCount(): Int {
        return getCheckedPositionList().size
    }

    /**
     * 获取单选索引
     */
    open fun getSingleCheckedPosition() = singleCheckIndex

    fun <T : DifferData> setList(scope: CoroutineScope, list: List<T>, defaultCheckPosition: Int) {
        if (checkedModel == CheckedModel.SINGLE) {
            singleCheckIndex = defaultCheckPosition
        }
        super.setPagingData(scope, PagingData.from(list))
    }

}