package cn.leo.paging_ktx.adapter

/*********************************************************************
 * Created by shenglei on 2023/4/26.
 *********************************************************************/
data class DecorationPadding(
    val hSpace: Int = 0,
    val vSpace: Int = 0,
    val hSide: Int = 0,                     //左边边界
    val vSide: Int = 0,                     //上边界
    val hSideRight: Int = -1,               //右边边界，-1表示和hSide一致
    val vSideBottom: Int = -1,              //下边界，-1表示和vSide一致
) {
    val leftSide: Int
        get() = hSide

    val rightSide: Int
        get() = if (hSideRight == -1) leftSide else hSideRight

    val topSide: Int
        get() = vSide

    val bottomSide: Int
        get() = if (vSideBottom == -1) topSide else vSideBottom
}