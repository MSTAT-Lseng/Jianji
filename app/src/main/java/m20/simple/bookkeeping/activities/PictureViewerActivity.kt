package m20.simple.bookkeeping.activities

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.panpf.sketch.loadImage
import com.github.panpf.zoomimage.SketchZoomImageView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.utils.FileUtils
import m20.simple.bookkeeping.utils.UIUtils

class PictureViewerActivity : AppCompatActivity() {

    private var defaultItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_viewer)

        UIUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        findViewById<ImageView>(R.id.back_btn).setOnClickListener {
            finish()
        }

        defaultItem = intent.getIntExtra("defaultItem", defaultItem)
        intent.getStringExtra("provideType")?.let { provideType ->
            loadImageList(initImageUri(provideType, intent.getStringExtra("images")?: ""))
        } ?: run {
            finish()
        }
    }

    private fun initImageUri(provideType: String, imageStr: String) : List<Uri> {
        var images: List<Uri> = emptyList()
        if (imageStr.isEmpty()) {
            return images
        }
        when (provideType) {
            "photos-storage" -> {
                val imageNames = imageStr.split(",").map { it.trim() }
                images = imageNames.mapNotNull { name ->
                    FileUtils(this@PictureViewerActivity).getPhotosUriByName(name)
                }
            }
            else -> {
                finish()
            }
        }
        return images
    }

    private fun loadImageList(images: List<Uri>) {
        val headerTextView = findViewById<TextView>(R.id.picture_number_text)
        // 设置适配器
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = ImagePagerAdapter(images)
        // 设置滑动事件监听器
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                headerTextView.text = getString(R.string.picture_view_number, "${position + 1} / ${images.size}")
            }
        })
        viewPager.setCurrentItem(defaultItem, false)
    }

}

// ViewPager2 的适配器
class ImagePagerAdapter(private val images: List<Uri>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sketchZoomImageView: SketchZoomImageView = itemView.findViewById(R.id.sketchZoomImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.picture_viewer_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.sketchZoomImageView.loadImage(images[position]) // 加载图片
    }

    override fun getItemCount(): Int = images.size
}