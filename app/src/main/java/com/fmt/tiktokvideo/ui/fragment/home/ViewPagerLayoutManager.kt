package com.fmt.tiktokvideo.ui.fragment.home

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

/**
 *  实现防抖音视频下拉翻页效果
 */
class ViewPagerLayoutManager(context: Context?, orientation: Int, reverseLayout: Boolean) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    private val mPagerSnapHelper = PagerSnapHelper()
    private var mOnViewPagerListener: OnViewPagerListener? = null
    private var mDrift = 0 //位移，用来判断移动方向

    /**
     *  检测当前视频列表的选择项
     */
    private val mChildAttachStateChangeListener: RecyclerView.OnChildAttachStateChangeListener =
        object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (mOnViewPagerListener != null && childCount == 1) {
                    mOnViewPagerListener!!.onInitComplete()
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (mDrift >= 0) {
                    if (mOnViewPagerListener != null) mOnViewPagerListener!!.onPageRelease(
                        true, getPosition(view)
                    )
                } else {
                    if (mOnViewPagerListener != null) mOnViewPagerListener!!.onPageRelease(
                        false, getPosition(view)
                    )
                }
            }
        }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        mPagerSnapHelper.attachToRecyclerView(view)
        view?.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener)
    }

    /**
     * 滑动状态的改变
     * 缓慢拖拽-> SCROLL_STATE_DRAGGING
     * 快速滚动-> SCROLL_STATE_SETTLING
     * 空闲状态-> SCROLL_STATE_IDLE
     */
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            val viewIdle = mPagerSnapHelper.findSnapView(this)
            if (viewIdle != null) {
                val positionIdle = getPosition(viewIdle)
                if (mOnViewPagerListener != null && childCount == 1) {
                    mOnViewPagerListener!!.onPageSelected(
                        positionIdle, positionIdle == itemCount - 1
                    )
                }
            }
        }
    }

    override fun scrollHorizontallyBy(
        dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?
    ): Int {
        this.mDrift = dx
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(
        dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?
    ): Int {
        this.mDrift = dy
        return super.scrollVerticallyBy(dy, recycler, state)
    }


    /**
     * 设置监听
     */
    fun setOnViewPagerListener(listener: OnViewPagerListener?) {
        this.mOnViewPagerListener = listener
    }

}