package com.fmt.tiktokvideo.nav

import android.content.ComponentName
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavGraphNavigator
import androidx.navigation.fragment.DialogFragmentNavigator
import com.fmt.nav_annotation.NavDestination
import com.fmt.tiktokvideo.ui.activity.FixFragmentNavigator

/**
 *  Navigation 导航构建器
 */
object NavGraphBuilder {

    /**
     *  构建navGraph路由表对象
     */
    fun build(
        controller: NavController,
        context: FragmentActivity,
        childFm: FragmentManager,
        containerId: Int
    ) {
        val provider = controller.navigatorProvider

        // 手动创建航图的管理者
        val navGraphNavigator = provider.getNavigator<NavGraphNavigator>("navigation")
        val navGraph = navGraphNavigator.createDestination()

        // 这里替换 fragment --》FragmentNavigator，源码中的实现：_navigators.put(name, navigator)
        val fixFragmentNavigator = FixFragmentNavigator(context, childFm, containerId)
        provider.addNavigator("fragment", fixFragmentNavigator)

        val iterator = NavRegistry.getNavList().listIterator()
        while (iterator.hasNext()) {
            val navData = iterator.next()
            when (navData.type) {
                NavDestination.NavType.Fragment -> {
                    val destination = fixFragmentNavigator.createDestination()
                    destination.id = navData.route.hashCode()
                    destination.setClassName(navData.className)
                    navGraph.addDestination(destination)
                }

                NavDestination.NavType.Activity -> {
                    val navigator = provider.getNavigator<ActivityNavigator>("activity")
                    val destination = navigator.createDestination()
                    destination.id = navData.route.hashCode()
                    destination.setComponentName(
                        ComponentName(
                            context.packageName,
                            navData.className
                        )
                    )
                    navGraph.addDestination(destination)
                }

                NavDestination.NavType.Dialog -> {
                    val navigator = provider.getNavigator<DialogFragmentNavigator>("dialog")
                    val destination = navigator.createDestination()
                    destination.id = navData.route.hashCode()
                    destination.setClassName(navData.className)
                    navGraph.addDestination(destination)
                }

                else -> {
                    throw java.lang.IllegalStateException("cant create NavGraph,because unknown ${navData.type}")
                }
            }
            // 设置第一个 Destination
            if (navData.asStarter) {
                navGraph.setStartDestination(navData.route.hashCode())
            }
        }
        // 设置路由表
        controller.setGraph(navGraph, null)
    }
}