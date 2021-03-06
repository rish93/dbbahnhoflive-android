package de.deutschebahn.bahnhoflive.ui.station

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.deutschebahn.bahnhoflive.backend.db.newsapi.model.News
import de.deutschebahn.bahnhoflive.view.ItemClickListener

class NewsAdapter(private val itemClickListener: ItemClickListener<News?>? = null) : RecyclerView.Adapter<NewsViewHolder>() {

    var newsList: List<News>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NewsViewHolder(parent, itemClickListener)

    override fun getItemCount() = newsList?.size ?: 0

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        newsList?.run {
            holder.bind(get(position))
        }
    }

}
