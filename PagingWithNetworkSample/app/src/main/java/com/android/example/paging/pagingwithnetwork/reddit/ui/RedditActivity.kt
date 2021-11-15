/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.paging.pagingwithnetwork.reddit.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.android.example.paging.pagingwithnetwork.GlideApp
import com.android.example.paging.pagingwithnetwork.databinding.ActivityRedditBinding
import com.android.example.paging.pagingwithnetwork.reddit.ServiceLocator
import com.android.example.paging.pagingwithnetwork.reddit.paging.asMergedLoadStates
import com.android.example.paging.pagingwithnetwork.reddit.repository.RedditPostRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

/**
 * A list activity that shows reddit posts in the given sub-reddit.
 * <p>
 * The intent arguments can be modified to make it use a different repository (see MainActivity).
 */
class RedditActivity : AppCompatActivity() {
    lateinit var binding: ActivityRedditBinding
        private set

    // viewModel工厂没有写成单个文件，vm写成单个文件
    private val model: SubRedditViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                // 页面传递参数
                val repoTypeParam = intent.getIntExtra(KEY_REPOSITORY_TYPE, 0)
                // 获取枚举值
                val repoType = RedditPostRepository.Type.values()[repoTypeParam]

                val repo = ServiceLocator.instance(this@RedditActivity)
                    .getRepository(repoType)
                @Suppress("UNCHECKED_CAST")
                return SubRedditViewModel(repo, handle) as T
            }
        }
    }

    private lateinit var adapter: PostsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRedditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAdapter()
        initSwipeToRefresh()
        initSearch()
    }

    private fun initAdapter() {
        val glide = GlideApp.with(this)
        adapter = PostsAdapter(glide)
        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostsLoadStateAdapter(adapter),
            footer = PostsLoadStateAdapter(adapter)
        )
        /**三个lifecycleScope.launchWhenCreated*/
        // 加载指示器显示loading
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collect { loadStates ->
                binding.swipeRefresh.isRefreshing = loadStates.mediator?.refresh is LoadState.Loading
            }
        }
        // adapter提交数据
        lifecycleScope.launchWhenCreated {
            model.posts.collectLatest {
                adapter.submitData(it)
            }
        }
        //
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Use a state-machine to track LoadStates such that we only transition to
                // NotLoading from a RemoteMediator load if it was also presented to UI.
                .asMergedLoadStates()
                // Only emit when REFRESH changes, as we only want to react on loads replacing the
                // list.
                    // 过滤掉连续重复的元素,不连续重复的是不过滤
                .distinctUntilChangedBy { it.refresh }
                    // 仅对刷新完成的情况作出反应，即不加载。
                // Only react to cases where REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                // Scroll to top is synchronous with UI updates, even if remote load was triggered.
                    // 滚动到顶部与UI更新同步，即使触发了远程加载。
                .collect { binding.list.scrollToPosition(0) }
        }
    }
    // 刷新
    private fun initSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }
    }
    // 查询
    private fun initSearch() {
        binding.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updatedSubredditFromInput()
                true
            } else {
                false
            }
        }
        binding.input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updatedSubredditFromInput()
                true
            } else {
                false
            }
        }
    }
    // 数据更新
    private fun updatedSubredditFromInput() {
        binding.input.text.trim().toString().let {
            if (it.isNotBlank()) {
                model.showSubreddit(it)
            }
        }
    }
    // 需要启动哪个页面，将intent方法放在哪个页面，封装的较好，可复用，不乱
    companion object {
        const val KEY_REPOSITORY_TYPE = "repository_type"
        fun intentFor(context: Context, type: RedditPostRepository.Type): Intent {
            val intent = Intent(context, RedditActivity::class.java)
            intent.putExtra(KEY_REPOSITORY_TYPE, type.ordinal)
            return intent
        }
    }
}
