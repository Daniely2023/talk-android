/*
 * Nextcloud Talk application
 *
 * @author Álvaro Brey
 * Copyright (C) 2022 Álvaro Brey
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.talk.polls.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import autodagger.AutoInjector
import com.nextcloud.talk.R
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.databinding.DialogPollResultsBinding
import com.nextcloud.talk.polls.adapters.PollResultItem
import com.nextcloud.talk.polls.adapters.PollResultItemClickListener
import com.nextcloud.talk.polls.adapters.PollResultsAdapter
import com.nextcloud.talk.polls.model.Poll
import com.nextcloud.talk.polls.viewmodels.PollResultsViewModel
import com.nextcloud.talk.polls.viewmodels.PollViewModel
import javax.inject.Inject

@AutoInjector(NextcloudTalkApplication::class)
class PollResultsFragment(
    private val parentViewModel: PollViewModel,
    private val roomToken: String,
    private val pollId: String
) : Fragment(), PollResultItemClickListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var viewModel: PollResultsViewModel

    var _binding: DialogPollResultsBinding? = null
    val binding: DialogPollResultsBinding
        get() = _binding!!

    private var adapter: PollResultsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[PollResultsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPollResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PollResultsAdapter(this)
        _binding?.pollResultsList?.adapter = adapter
        _binding?.pollResultsList?.layoutManager = LinearLayoutManager(context)

        parentViewModel.viewState.observe(viewLifecycleOwner) { state ->
            if (state is PollViewModel.PollVotedState &&
                state.poll.resultMode == Poll.RESULT_MODE_PUBLIC
            ) {

                initPollResults(state.poll)
                initAmountVotersInfo(state)
                initEditButton(state)
            } else if (state is PollViewModel.PollUnvotedState &&
                state.poll.status == Poll.STATUS_CLOSED
            ) {
                Log.d(TAG, "show results also if self never voted")
            }

        }
    }

    private fun initPollResults(poll: Poll) {
        if (poll.details != null) {
            val votersAmount = poll.details.size
            val oneVoteInPercent = 100 / votersAmount  // TODO: poll.numVoters   when fixed on api

            poll.options?.forEachIndexed { index, option ->
                val votersForThisOption = poll.details.filter { it.optionId == index }.size
                val optionsPercent = oneVoteInPercent * votersForThisOption

                val pollResultItem = PollResultItem(option, optionsPercent) // TODO add participants to PollResultItem
                adapter?.list?.add(pollResultItem)
            }
        } else if (poll.votes != null) {
            val votersAmount = poll.numVoters
            val oneVoteInPercent = 100 / votersAmount

            poll.options?.forEachIndexed { index, option ->
                val votersForThisOption = poll.votes?.filter { it.key.toInt() == index }?.size!!
                val optionsPercent = oneVoteInPercent * votersForThisOption

                val pollResultItem = PollResultItem(option, optionsPercent)
                adapter?.list?.add(pollResultItem)
            }
        } else {
            Log.e(TAG, "failed to get data to show poll results")
        }
    }

    private fun initAmountVotersInfo(state: PollViewModel.PollVotedState) {
        _binding?.pollAmountVoters?.text = String.format(
            resources.getString(R.string.polls_amount_voters),
            state.poll.numVoters
        )
    }

    private fun initEditButton(state: PollViewModel.PollVotedState) {
        if (state.poll.status == Poll.STATUS_OPEN && state.poll.resultMode == Poll.RESULT_MODE_PUBLIC) {
            _binding?.editVoteButton?.visibility = View.VISIBLE
            _binding?.editVoteButton?.setOnClickListener {
                parentViewModel.edit()
            }
        } else {
            _binding?.editVoteButton?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = PollResultsFragment::class.java.simpleName
    }

    override fun onClick(pollResultItem: PollResultItem) {
        Log.d(TAG, "click..")
    }
}
