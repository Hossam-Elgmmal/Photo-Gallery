package com.bignerdranch.android.photogallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bignerdranch.android.photogallery.databinding.FragmentPhotoGalleryBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"

class PhotoGalleryFragment : Fragment() {

    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val photoGalleryViewModel: PhotoGalleryViewModel by viewModels()
    private var searchView: SearchView? = null
    private var pollingMenuItem: MenuItem? = null

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.UNMETERED)
//            .build()
//        val workRequest =
//            OneTimeWorkRequest.Builder(PollWorker::class.java)
//                .setConstraints(constraints)
//                .build()
//        WorkManager.getInstance(requireContext())
//            .enqueue(workRequest)
//
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentPhotoGalleryBinding.inflate(inflater, container, false)
        binding.photoGrid.layoutManager = GridLayoutManager(context, 3)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                photoGalleryViewModel.uiState.collect { state ->

                    binding.photoGrid.adapter =
                        PhotoListAdapter(state.images) { photoPageUri ->
                            //val intent = Intent(Intent.ACTION_VIEW, photoPageUri)
                            //startActivity(intent)
                            findNavController().navigate(
                                PhotoGalleryFragmentDirections.showPhoto(
                                    photoPageUri
                                )
                            )
                        }
                    searchView?.setQuery(state.query, false)

                    updatePollingState(state.isPolling)
                }
            }
        }

        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_photo_gallery, menu)

                    val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
                    searchView = searchItem.actionView as? SearchView
                    pollingMenuItem = menu.findItem(R.id.menu_item_toggle_polling)

                    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                        override fun onQueryTextSubmit(query: String?): Boolean {
                            Log.d(TAG, "onQueryTextSubmit: $query")
                            photoGalleryViewModel.setQuery(query ?: "")
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            Log.d(TAG, "onQueryTextChange: $newText")
                            return false
                        }

                    })
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.menu_item_clear -> {
                            photoGalleryViewModel.setQuery("")
                            true
                        }

                        R.id.menu_item_toggle_polling -> {
                            photoGalleryViewModel.toggleIsPolling()
                            true
                        }

                        else -> false
                    }
                }

            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    private fun updatePollingState(isPolling: Boolean) {
        val toggleItemTitle =
            if (isPolling) R.string.stop_polling
            else R.string.start_polling

        pollingMenuItem?.setTitle(toggleItemTitle)

        if (isPolling) {
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            val periodicRequest =
                PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                POLL_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } else {
            WorkManager.getInstance(requireContext()).cancelUniqueWork(POLL_WORK)
        }
    }

}