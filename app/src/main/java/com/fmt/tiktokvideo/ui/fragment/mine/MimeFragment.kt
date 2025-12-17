package com.fmt.tiktokvideo.ui.fragment.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fmt.nav_annotation.NavDestination
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.cache.UserManager
import com.fmt.tiktokvideo.http.URLs.LOVE_URL
import com.fmt.tiktokvideo.http.URLs.WORK_URL
import com.fmt.tiktokvideo.model.Users
import com.fmt.tiktokvideo.nav.Router
import com.fmt.tiktokvideo.ui.adapter.formatCount
import com.fmt.tiktokvideo.ui.viewmodel.MineViewModel
import com.fmt.tiktokvideo.view.LoadingStatusView
import kotlinx.coroutines.launch

/**
 *  个人中心页面
 */
@NavDestination(type = NavDestination.NavType.Fragment, route = Router.MINE)
class MimeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).also {
            it.setContent {
                ProfileScreen()
            }
        }
    }

    @Composable
    fun ProfileScreen() {
        val viewModel: MineViewModel = viewModel()
        var showLoginOutDialog by remember { mutableStateOf(false) }
        var author by remember { mutableStateOf(Users()) }
        var selectedIndex by remember { mutableIntStateOf(0) }
        val dataList = if (selectedIndex == 0) viewModel.workItems else viewModel.loveItems
        val refreshingState =
            if (selectedIndex == 0) viewModel.refreshingWorkState else viewModel.refreshingLoveState
        val loadMoreState =
            if (selectedIndex == 0) viewModel.loadMoreWorkState else viewModel.loadMoreLoveState
        val pullToRefreshState = rememberPullToRefreshState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(selectedIndex) {
            val reqUrl = if (selectedIndex == 0) WORK_URL else LOVE_URL
            // 首次或手动刷新时再请求；切换时优先复用已缓存的数据
            if (dataList.isEmpty()) {
                viewModel.getTabData(reqUrl, isRefresh = false)
            }
        }

        LaunchedEffect(Unit) {
            scope.launch {
                // 获取用户信息
                UserManager.getUser().collect {
                    author = it
                }
            }
        }

        if (showLoginOutDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLoginOutDialog = false
                },
                title = {
                    Text(text = "确认退出吗？")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLoginOutDialog = false
                            UserManager.logout()
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showLoginOutDialog = false
                        },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text("取消")
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // PullToRefreshBox 检测整个内容区域的下拉手势。如果被其他容器包裹，手势可能被拦截或无法正确传递，注意布局层次
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = refreshingState.value,
            onRefresh = {
                viewModel.getTabData(
                    if (selectedIndex == 0) WORK_URL else LOVE_URL, isRefresh = true
                )
            },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = refreshingState.value,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 290.dp),
                    color = Color.Black
                )
            }) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ProfileHeader(author) {
                            showLoginOutDialog = true
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SecondaryTabRow(
                            selectedTabIndex = selectedIndex,
                            modifier = Modifier
                                .height(50.dp)
                                .shadow(3.dp),
                            containerColor = Color.White,
                            contentColor = Color.White,
                            divider = {},
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(
                                        selectedIndex, matchContentSize = false
                                    ), height = 2.dp, color = Color.Black
                                )
                            }) {
                            var selected = selectedIndex == 0
                            Tab(
                                selected = selected,
                                content = {
                                    Text(
                                        text = "作品",
                                        fontSize = 16.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { selectedIndex = 0 },
                                selectedContentColor = Color.Black,
                                unselectedContentColor = Color.Gray,
                                modifier = Modifier.height(48.dp)
                            )
                            selected = selectedIndex == 1
                            Tab(
                                selected = selected,
                                content = {
                                    Text(
                                        text = "喜欢",
                                        fontSize = 16.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { selectedIndex = 1 },
                                selectedContentColor = Color.Black,
                                unselectedContentColor = Color.Gray,
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                    val dataList =
                        if (selectedIndex == 0) viewModel.workItems else viewModel.loveItems
                    itemsIndexed(dataList) { index, videoItem ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 1.dp)
                                .wrapContentHeight(), contentAlignment = Alignment.BottomStart
                        ) {
                            AsyncImage(
                                model = videoItem.data?.cover?.feed,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentDescription = null,
                            )
                            Row(
                                modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.mipmap.icon_tiktok_collect),
                                    modifier = Modifier.size(16.dp),
                                    contentDescription = null
                                )
                                videoItem.data?.consumption?.let { consumption ->
                                    Text(
                                        text = formatCount(consumption.collectionCount),
                                        modifier = Modifier.padding(start = 3.dp),
                                        color = Color.White
                                    )
                                }
                            }
                            // 加载更多请求实现
                            if (index >= dataList.size - 1) {
                                LaunchedEffect(dataList.size) {
                                    viewModel.loadMoreData()
                                }
                            }
                        }
                    }
                    // 加载更多控件显示
                    if (dataList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadingMoreItem(loadMoreState.value)
                        }
                    }
                }
                // 默认空布局
                if (dataList.isEmpty()) {
                    AndroidView(
                        factory = { LoadingStatusView(it) },
                        modifier = Modifier
                            .padding(top = 180.dp)
                            .fillMaxSize()
                            .align(Alignment.Center),
                        update = { view: LoadingStatusView ->
                            view.isVisible = true
                            view.showEmpty(
                                tintColorRes = android.R.color.black,
                                emptyTxtColorRes = android.R.color.black,
                                retryTxtColorRes = android.R.color.black,
                                retry = {
                                    val reqUrl = if (selectedIndex == 0) WORK_URL else LOVE_URL
                                    viewModel.getTabData(reqUrl, isRefresh = true)
                                })
                        })
                }
            }
        }
    }

    /**
     *  顶部头像背景
     */
    @Composable
    fun ProfileHeader(author: Users, onShowLoginOut: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_default_header_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .height(240.dp),
                contentDescription = null
            )

            Image(
                painter = painterResource(R.mipmap.icon_logout),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .padding(top = 36.dp, end = 15.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clickable(true) {
                        onShowLoginOut()
                    },
                contentDescription = null
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {
                AsyncImage(
                    model = author.avatar,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .align(Alignment.CenterHorizontally),
                    contentDescription = null,
                )
                Text(
                    text = author.nickname,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    /**
     *  加载更多控件
     */
    @Composable
    fun LoadingMoreItem(loadMoreState: Boolean) {
        if (loadMoreState) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 15.dp)
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "正在加载中...")
            }
        } else {
            Text(
                text = "已经没有更多了",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 15.dp)
                    .fillMaxWidth()
                    .height(45.dp)
            )
        }
    }
}