package com.arvind.newsapp.ui.fragments

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AbsListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arvind.newsapp.R
import com.arvind.newsapp.adapters.NewsAdapters
import com.arvind.newsapp.db.ArticleDatabase
import com.arvind.newsapp.repository.NewsRepository
import com.arvind.newsapp.ui.NewsActivity
import com.arvind.newsapp.ui.NewsViewModel
import com.arvind.newsapp.ui.NewsViewModelProviderFactory
import com.arvind.newsapp.util.Constants.Companion.QUERY_PAGE_SIZE
import com.arvind.newsapp.util.Resource
import kotlinx.android.synthetic.main.fragment_breaking_news.paginationProgressBar
import kotlinx.android.synthetic.main.fragment_breaking_news.rvBreakingNews
import java.io.File

class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news) {
    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapters
    val TAG = "BreakingNewsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        viewModel = (activity as NewsActivity).viewModel
//                if (this::viewModel.isInitialized){
        val  newsRepository = NewsRepository(ArticleDatabase(requireActivity().applicationContext))
        val newsViewModelProviderFactory = NewsViewModelProviderFactory(requireActivity().application,newsRepository)
        viewModel = ViewModelProvider(this,newsViewModelProviderFactory).get(NewsViewModel::class.java)

        setUpRecyclerView()
        newsAdapter.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("article",it)
            }
            findNavController().navigate(
                R.id.action_breakingNewsFragment_to_articleFragment,
                bundle
            )
        }

            viewModel.breakingNews.observe(viewLifecycleOwner, Observer { response ->

                when (response) {
                    is Resource.Success -> {
                        hideProgressBar()
                        response.data?.let {newsResponse ->
                            newsAdapter.differ.submitList(newsResponse.articles.toList())
                            val totalPages = newsResponse.totalResults / QUERY_PAGE_SIZE + 2
                            isLastPage = viewModel.breakingNewsPage ==totalPages
                            if(isLastPage){
                                rvBreakingNews.setPadding(0,0,0,0)
                            }
                        }
                    }
                    is Resource.Error ->{
                        hideProgressBar()
                        response.message?.let{message ->
                            Toast.makeText(activity,"An error occurred: $message",Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "An error occurred: $message" )
                        }
                    }
                    is Resource.Loading ->{
                        showProgressBar()
                    }
                }
            })
//        }


    }

    private fun hideProgressBar() {
        paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

   private fun showProgressBar() {
        paginationProgressBar.visibility = View.VISIBLE
       isLoading = true
    }

    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    val scrollListener = object :RecyclerView.OnScrollListener(){
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager

            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount  = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBegning = firstVisibleItemPosition >=0
            val isTotalMoreThanVisible = totalItemCount >=QUERY_PAGE_SIZE
            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBegning && isTotalMoreThanVisible && isScrolling
            if (shouldPaginate){
                viewModel.getBreakingNews("in")
                isScrolling = false
            }
        }
    }

    fun setUpRecyclerView() {
        newsAdapter = NewsAdapters()
        rvBreakingNews.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@BreakingNewsFragment.scrollListener)
        }
    }
}