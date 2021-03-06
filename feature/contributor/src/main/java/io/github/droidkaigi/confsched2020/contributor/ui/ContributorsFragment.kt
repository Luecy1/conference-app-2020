package io.github.droidkaigi.confsched2020.contributor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.databinding.ViewHolder
import dagger.Component
import dagger.Module
import dagger.Provides
import io.github.droidkaigi.confsched2020.App
import io.github.droidkaigi.confsched2020.di.AppComponent
import io.github.droidkaigi.confsched2020.di.PageScope
import io.github.droidkaigi.confsched2020.ext.assistedViewModels
import io.github.droidkaigi.confsched2020.contributor.R
import io.github.droidkaigi.confsched2020.contributor.databinding.FragmentContributorsBinding
import io.github.droidkaigi.confsched2020.contributor.ui.di.ContributorAssistedInjectModule
import io.github.droidkaigi.confsched2020.contributor.ui.viewmodel.ContributorsViewModel
import io.github.droidkaigi.confsched2020.util.ProgressTimeLatch
import javax.inject.Inject
import javax.inject.Provider

class ContributorsFragment : Fragment() {

    private lateinit var binding: FragmentContributorsBinding

    @Inject lateinit var contributorsFactory: Provider<ContributorsViewModel>
    private val contributorsViewModel by assistedViewModels {
        contributorsFactory.get()
    }

    private lateinit var progressTimeLatch: ProgressTimeLatch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_contributors,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appComponent = (requireContext().applicationContext as App).appComponent
        val component = DaggerContributorComponent.builder()
            .appComponent(appComponent)
            .contributorModule(ContributorModule(this))
            .build()
        component.inject(this)

        val groupAdapter = GroupAdapter<ViewHolder<*>>()
        binding.contributorRecycler.adapter = groupAdapter

        progressTimeLatch = ProgressTimeLatch { showProgress ->
            binding.progressBar.isVisible = showProgress
        }.apply {
            loading = true
        }
        // TODO Implement
//        contributorsViewModel.uiModel.observe(viewLifecycleOwner) { uiModel ->
//
//        }
    }
}

@Module
class ContributorModule(private val fragment: ContributorsFragment) {
    @PageScope @Provides
    fun providesLifecycleOwnerLiveData(): LiveData<LifecycleOwner> {
        return fragment.viewLifecycleOwnerLiveData
    }
}

@PageScope
@Component(
    modules = [
        ContributorModule::class,
        ContributorAssistedInjectModule::class
    ],
    dependencies = [AppComponent::class]
)
interface ContributorComponent {
    @Component.Builder
    interface Builder {
        fun build(): ContributorComponent
        fun appComponent(appComponent: AppComponent): Builder
        fun contributorModule(contributorModule: ContributorModule): Builder
    }

    fun inject(fragment: ContributorsFragment)
}
