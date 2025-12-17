package com.fmt.tiktokvideo.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.Jzvd
import com.fmt.nav_annotation.NavDestination
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.FragmentHomeBinding
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.ext.invokeViewModel
import com.fmt.tiktokvideo.nav.Router
import com.fmt.tiktokvideo.ui.adapter.FooterLoadStateAdapter
import com.fmt.tiktokvideo.ui.adapter.HomeAdapter
import com.fmt.tiktokvideo.ui.viewmodel.HomeViewModel
import com.fmt.tiktokvideo.view.TikTokVideoView
import kotlinx.coroutines.launch

/**
 *  视频列表页面
 */
@NavDestination(type = NavDestination.NavType.Fragment, route = Router.HOME, asStarter = true)
class HomeFragment : Fragment(R.layout.fragment_home), OnViewPagerListener {

    private val mBinding: FragmentHomeBinding by invokeViewBinding<FragmentHomeBinding>()
    private val mViewModel: HomeViewModel by invokeViewModel<HomeViewModel>()
    private lateinit var mHomeAdapter: HomeAdapter
    private var mCurrentPosition = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        mHomeAdapter = HomeAdapter(this)
        val contactAdapter = mHomeAdapter.withLoadStateFooter(FooterLoadStateAdapter {
            mHomeAdapter.retry()
        })
        val viewPagerLayoutManager =
            ViewPagerLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mBinding.listView.layoutManager = viewPagerLayoutManager
        viewPagerLayoutManager.setOnViewPagerListener(this)
        mBinding.listView.adapter = contactAdapter
        mBinding.listView.addOnChildAttachStateChangeListener(object :
            RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                val jzvd = view.findViewById<Jzvd>(R.id.video_view)
                if (jzvd != null && Jzvd.CURRENT_JZVD != null && jzvd.jzDataSource != null &&
                    jzvd.jzDataSource.containsTheUrl(Jzvd.CURRENT_JZVD.jzDataSource.currentUrl)
                ) {
                    if (Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.screen != Jzvd.SCREEN_FULLSCREEN) {
                        Jzvd.releaseAllVideos()
                    }
                }
            }
        })

        mBinding.refreshLayout.setColorSchemeColors(requireContext().getColor(R.color.black))
        mBinding.refreshLayout.setOnRefreshListener {
            mHomeAdapter.refresh()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.pageFlow.collect {
                    mHomeAdapter.submitData(it)
                }
            }
        }

        lifecycleScope.launch {
            // 监听列表数据加载状态
            mHomeAdapter.onPagesUpdatedFlow.collect {
                val hasData = mHomeAdapter.itemCount > 0
                mBinding.refreshLayout.isRefreshing = false
                mBinding.listView.isVisible = hasData
                mBinding.loadingStatus.isVisible = !hasData
                // 当列表没有展示任务数据时，展示出兜底页
                if (!hasData) {
                    mBinding.loadingStatus.showEmpty(retry = {
                        mHomeAdapter.refresh()
                    })
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()
    }

    override fun onResume() {
        super.onResume()
        Jzvd.goOnPlayOnResume()
    }

    override fun onInitComplete() {
        //自动播放第一条
        autoPlayVideo(0)
    }

    override fun onPageRelease(isNext: Boolean, position: Int) {
        if (mCurrentPosition == position) {
            Jzvd.releaseAllVideos()
        }
    }

    override fun onPageSelected(position: Int, isBottom: Boolean) {
        if (mCurrentPosition == position) {
            return
        }
        autoPlayVideo(position)
        mCurrentPosition = position
    }

    /**
     *  自动播放列表中的第一个可见区域的视频
     */
    private fun autoPlayVideo(position: Int) {
        if (mBinding.listView.getChildAt(0) == null) {
            return
        }
        val player = mBinding.listView.getChildAt(0).findViewById<TikTokVideoView>(R.id.video_view)
        player?.startVideoAfterPreloading()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            Jzvd.goOnPlayOnPause()
        } else {
            Jzvd.goOnPlayOnResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Jzvd.releaseAllVideos()
    }
}