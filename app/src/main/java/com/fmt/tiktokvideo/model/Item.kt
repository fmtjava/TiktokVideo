package com.fmt.tiktokvideo.model

data class Item(
    val type: String = "",
    val data: ItemData? = null,
)

data class ItemData(
    val id: Int,
    val dataType: String,
    val text: String? = "",
    val description: String = "",
    val title: String,
    val category: String,
    val author: Author?,
    val cover: Cover,
    val duration: Int,
    val header: Header?,
    val itemList: List<Item>?,
    val width: Int,
    val height: Int,
    val owner: Owner?,
    val consumption: Consumption,
    val urls: List<String>?,
    val playUrl: String,
)

data class Header(val id: Int, val icon: String, val title: String, val description: String)
data class Author(
    val icon: String,
    val name: String,
    val description: String,
    val latestReleaseTime: Long,
)

data class Cover(
    val feed: String,
    val blurred: String,
    val detail: String,
)

data class Owner(
    val avatar: String,
    val nickname: String,
)

data class Consumption(
    val collectionCount: Int,
    val shareCount: Int,
    val replyCount: Int,
    val realCollectionCount: Int,
)