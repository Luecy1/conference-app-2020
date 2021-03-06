package io.github.droidkaigi.confsched2020.sponsor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.databinding.ViewHolder
import dagger.Module
import dagger.Provides
import dagger.android.support.DaggerFragment
import io.github.droidkaigi.confsched2020.di.PageScope
import io.github.droidkaigi.confsched2020.ext.assistedActivityViewModels
import io.github.droidkaigi.confsched2020.ext.assistedViewModels
import io.github.droidkaigi.confsched2020.model.Sponsor
import io.github.droidkaigi.confsched2020.model.SponsorCategory
import io.github.droidkaigi.confsched2020.sponsor.R
import io.github.droidkaigi.confsched2020.sponsor.databinding.FragmentSponsorsBinding
import io.github.droidkaigi.confsched2020.sponsor.ui.item.CategoryHeaderItem
import io.github.droidkaigi.confsched2020.sponsor.ui.item.LargeSponsorItem
import io.github.droidkaigi.confsched2020.sponsor.ui.item.SponsorItem
import io.github.droidkaigi.confsched2020.sponsor.ui.viewmodel.SponsorsViewModel
import io.github.droidkaigi.confsched2020.system.ui.viewmodel.SystemViewModel
import io.github.droidkaigi.confsched2020.util.ProgressTimeLatch
import javax.inject.Inject
import javax.inject.Provider

class SponsorsFragment : DaggerFragment() {

    private lateinit var binding: FragmentSponsorsBinding

    @Inject lateinit var sponsorsModelFactory: Provider<SponsorsViewModel>
    private val sponsorsViewModel by assistedViewModels {
        sponsorsModelFactory.get()
    }
    @Inject lateinit var systemViewModelProvider: Provider<SystemViewModel>
    private val systemViewModel: SystemViewModel by assistedActivityViewModels {
        systemViewModelProvider.get()
    }

    @Inject lateinit var largeSponsorItemFactory: LargeSponsorItem.Factory

    @Inject lateinit var sponsorItemFactory: SponsorItem.Factory

    @Inject lateinit var categoryHeaderItemFactory: CategoryHeaderItem.Factory

    private lateinit var progressTimeLatch: ProgressTimeLatch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_sponsors,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupAdapter = GroupAdapter<ViewHolder<*>>()
        groupAdapter.spanCount = 2

        binding.sponsorRecycler.layoutManager = GridLayoutManager(
            requireContext(),
            groupAdapter.spanCount
        ).apply {
            spanSizeLookup = groupAdapter.spanSizeLookup
        }
        binding.sponsorRecycler.adapter = groupAdapter

        progressTimeLatch = ProgressTimeLatch { showProgress ->
            binding.progressBar.isVisible = showProgress
        }.apply {
            loading = true
        }
        sponsorsViewModel.uiModel.observe(viewLifecycleOwner) { uiModel ->
            progressTimeLatch.loading = uiModel.isLoading
            groupAdapter.update(
                uiModel.sponsorCategories.map {
                    it.toSection()
                    // TODO: Add FooterItem() if needed.
                }
            )
            uiModel.error?.let {
                systemViewModel.onError(it)
            }
        }
    }

    private fun SponsorCategory.toSection() = Section().apply {
        setHeader(categoryHeaderItemFactory.create(category))
        addAll(
            sponsors.map { sponsor ->
                sponsor.toItem(category)
            }
        )
        setHideWhenEmpty(true)
    }

    private fun Sponsor.toItem(category: SponsorCategory.Category): Item<*> {
        val spanSize = when (category) {
            SponsorCategory.Category.PLATINUM -> 2
            else -> 1
        }
        return when (category) {
            SponsorCategory.Category.PLATINUM,
            SponsorCategory.Category.GOLD -> {
                largeSponsorItemFactory.create(this, spanSize, systemViewModel)
            }
            else -> {
                sponsorItemFactory.create(this, spanSize, systemViewModel)
            }
        }
    }
}

@Module
abstract class SponsorsFragmentModule {
    @Module
    companion object {
        @PageScope
        @JvmStatic @Provides fun providesLifeCycleLiveData(
            sponsorsFragment: SponsorsFragment
        ): LiveData<LifecycleOwner> {
            return sponsorsFragment.viewLifecycleOwnerLiveData
        }
    }
}