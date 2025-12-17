package com.fmt.tiktokvideo.model

data class Daily(val issueList: MutableList<Issue>, val nextPageUrl: String? = null)