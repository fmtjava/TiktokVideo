package com.fmt.nav_annotation

/**
 *  导航注解：type        --> 导航页面类型
 *          route       --> 导航页面名称
 *          asStarter   --> 是否为第一个 Destination
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class NavDestination(
    val type: NavType,
    val route: String,
    val asStarter: Boolean = false
) {
    enum class NavType {
        Fragment,
        Activity,
        Dialog,
        None
    }
}