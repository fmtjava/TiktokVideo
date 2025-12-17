package com.fmt.nav_annotation

/**
 *  每个导航页面映射的 Model
 *      type        --> 导航页面类型
 *      route       --> 导航页面名称
 *      asStarter   --> 是否为第一个 Destination
 *      className   --> 导航页面全类名
 */
data class NavData(
    val route: String,
    val className: String,
    val asStarter: Boolean,
    val type: NavDestination.NavType
)