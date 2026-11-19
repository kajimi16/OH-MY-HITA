package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ScoreEntry
import com.stupidtree.hita.api.ScoreReport
import com.stupidtree.hita.api.ScoreService
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.source.web.service.EASService

internal class ScoreServiceImpl(private val application: Application) : ScoreService {

    private val repo get() = EASRepository.getInstance(application)

    override fun terms(): LiveData<DataState<List<TermItem>>> = repo.getAllTerms()

    override fun report(
        term: TermItem,
        includeResit: Boolean,
    ): LiveData<DataState<ScoreReport>> {
        val type = if (includeResit) EASService.TestType.ALL else EASService.TestType.NORMAL
        return repo.getPersonalScoresWithSummary(term, type).map { state ->
            val data = state.data
            if (state.state != DataState.STATE.SUCCESS || data == null) {
                return@map DataState<ScoreReport>(state.state).apply { message = state.message }
            }
            DataState(
                ScoreReport(
                    term = term,
                    gpa = data.summary?.gpa.orEmpty(),
                    rank = data.summary?.rank.orEmpty(),
                    total = data.summary?.total.orEmpty(),
                    entries = data.items.map {
                        ScoreEntry(
                            courseName = it.courseName.orEmpty(),
                            credits = it.credits,
                            score = it.finalScores,
                            termName = it.termName,
                        )
                    },
                ),
                DataState.STATE.SUCCESS,
            )
        }
    }
}
