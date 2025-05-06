package com.example.wificonnect

import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Représente un fichier image ou vidéo
data class GalleryItem(val url: String, val type: String, val name: String)

class GalleryAdapter(
    private val context: Context,
    private val items: List<GalleryItem>
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val playIcon: ImageView = view.findViewById(R.id.play_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.type == "video") {
            // Affiche une icône ou image fixe pour les vidéos
            holder.thumbnail.setImageResource(R.drawable.ic_video_placeholder)
        } else {
            // Charge uniquement les images avec Glide
            Glide.with(context)
                .load(item.url)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.thumbnail)
        }

        holder.playIcon.visibility = if (item.type == "video") View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val intent = Intent(context, FilePreviewActivity::class.java).apply {
                putExtra("fileUrl", item.url)
                putExtra("fileType", item.type)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}