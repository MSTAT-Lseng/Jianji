package m20.simple.bookkeeping.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.utils.UIUtils

class BillingInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_info)

        UIUtils().setStatusBarTextColor(this, !UIUtils().isDarkMode(resources))

    }

}