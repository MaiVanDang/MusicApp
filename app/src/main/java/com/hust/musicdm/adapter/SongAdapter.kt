package com.hust.musicdm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hust.musicdm.R
import com.hust.musicdm.databinding.ItemSongBinding
import com.hust.musicdm.model.Song

class SongAdapter(
    private val onSongClick: (Song, Int, List<Song>) -> Unit,
    private val onDownloadClick: (Song) -> Unit,
    private val onDeleteClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    private var downloadProgress = mapOf<Long, Int>()

    fun updateDownloadProgress(progressMap: Map<Long, Int>) {
        downloadProgress = progressMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        val progress = downloadProgress[song.id]
        holder.bind(song, progress, currentList, onSongClick, onDownloadClick, onDeleteClick)
    }

    class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            song: Song,
            downloadProgress: Int?,
            allSongs: List<Song>,
            onSongClick: (Song, Int, List<Song>) -> Unit,
            onDownloadClick: (Song) -> Unit,
            onDeleteClick: (Song) -> Unit
        ) {
            binding.tvSongTitle.text = song.title ?: "Unknown"
            binding.tvArtist.text = song.artist ?: "Unknown Artist"

            // Load album art
            Glide.with(binding.root.context)
                .load(song.albumArt)
                .placeholder(R.drawable.baseline_music_note_24)
                .error(R.drawable.baseline_music_note_24)
                .into(binding.ivAlbumArt)

            // Handle download states
            when {
                downloadProgress != null -> {
                    // Downloading
                    binding.btnDownload.setImageResource(R.drawable.ic_downloading_progress)
                    binding.btnDownload.clearAnimation()
                    val rotation = AnimationUtils.loadAnimation(
                        binding.root.context,
                        R.anim.rotate_animation
                    )
                    binding.btnDownload.startAnimation(rotation)
                    binding.btnDownload.isEnabled = false
                    binding.tvDownloadProgress.visibility = View.VISIBLE
                    binding.tvDownloadProgress.text = "$downloadProgress%"
                }
                song.isDownloaded -> {
                    // Downloaded
                    binding.btnDownload.clearAnimation()
                    binding.btnDownload.setImageResource(R.drawable.outline_download_done_24)
                    binding.btnDownload.isEnabled = true
                    binding.tvDownloadProgress.visibility = View.GONE
                }
                else -> {
                    // Not downloaded
                    binding.btnDownload.clearAnimation()
                    binding.btnDownload.setImageResource(R.drawable.outline_download_24)
                    binding.btnDownload.isEnabled = true
                    binding.tvDownloadProgress.visibility = View.GONE
                }
            }

            // Click listeners
            binding.btnDownload.setOnClickListener {
                when {
                    downloadProgress != null -> {
                        // Đang tải - không làm gì
                    }
                    song.isDownloaded -> {
                        // Đã tải - GỌI CALLBACK XÓA
                        onDeleteClick(song)
                    }
                    else -> {
                        // Chưa tải - tải xuống
                        onDownloadClick(song)
                    }
                }
            }

            binding.root.setOnClickListener {
                onSongClick(song, bindingAdapterPosition, allSongs)
            }
        }
    }

    private class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}