package m20.simple.bookkeeping.ui.home

import android.annotation.SuppressLint
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import m20.simple.bookkeeping.worker.SchedPlanWorker

class HomeViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private var toolbar: Toolbar? = null
    private var schedPlanWorker: SchedPlanWorker? = null

    val toolbarMessage = MutableLiveData<Boolean>()

    fun getToolbar(): Toolbar? {
        return toolbar
    }

    fun setToolbar(toolbar: Toolbar?) {
        this.toolbar = toolbar
    }

    fun getSchedPlanWorker(): SchedPlanWorker? {
        return schedPlanWorker
    }

    fun setSchedPlanWorker(schedPlanWorker: SchedPlanWorker) {
        this.schedPlanWorker = schedPlanWorker
    }

}