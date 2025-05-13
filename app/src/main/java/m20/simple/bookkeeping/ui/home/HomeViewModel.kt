package m20.simple.bookkeeping.ui.home

import android.annotation.SuppressLint
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private var toolbar: Toolbar? = null

    val toolbarMessage = MutableLiveData<Boolean>()

    fun getToolbar(): Toolbar? {
        return toolbar
    }

    fun setToolbar(toolbar: Toolbar?) {
        this.toolbar = toolbar
    }

}