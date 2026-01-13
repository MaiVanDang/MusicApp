package com.hust.musicdm.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.hust.musicdm.R
import com.hust.musicdm.adapter.SongAdapter
import com.hust.musicdm.api.DownloadManager
import com.hust.musicdm.api.RetrofitClient
import com.hust.musicdm.database.SongDatabase
import com.hust.musicdm.databinding.ActivityMainBinding
import com.hust.musicdm.database.UserManager
import com.hust.musicdm.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var userManager: UserManager
    private lateinit var database: SongDatabase
    private lateinit var downloadManager: DownloadManager

    private val downloadProgressMap = mutableMapOf<Long, Int>()
    private var fullSongList: List<Song> = emptyList()
    private var isOfflineMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)
        database = SongDatabase.getDatabase(this)
        downloadManager = DownloadManager(this)

        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupSwipeRefresh()

        loadSongs()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigationDrawer() {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)

        val currentUser = userManager.getCurrentUser()
        tvName.text = currentUser?.fullName ?: "Guest"
        tvEmail.text = currentUser?.email ?: ""

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    isOfflineMode = false
                    songAdapter.submitList(fullSongList)
                    binding.toolbar.title = "MusicDM"
                }
                R.id.nav_offline -> {
                    isOfflineMode = true
                    filterListSongsDownloaded()
                }
                R.id.nav_logout -> {
                    userManager.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun filterListSongsDownloaded() {
        val listSongsDownloaded = fullSongList.filter { it.isDownloaded }
        if (listSongsDownloaded.isEmpty()) {
            Toast.makeText(this, "Chưa có bài hát nào được tải về", Toast.LENGTH_SHORT).show()
        }
        songAdapter.submitList(listSongsDownloaded)
        binding.toolbar.title = "Nhạc đã tải"
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song, position, list ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("songList", ArrayList(list))
                    putExtra("position", position)
                }
                startActivity(intent)
            },
            onDownloadClick = { song ->
                startDownload(song)
            },
            onDeleteClick = { song -> // THÊM callback xóa
                showDeleteConfirmDialog(song)
            }
        )
        binding.rvSongs.adapter = songAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadSongs()
        }
    }

    private fun loadSongs() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getSongs()
                if (response.isSuccessful) {
                    val apiSongs = response.body() ?: emptyList()

                    // Kiểm tra trạng thái tải xuống từ Database
                    withContext(Dispatchers.IO) {
                        // Lấy danh sách bài hát đã có trong database
                        val entities = database.songDao().getAllSongsSync()
                        val entityMap = entities.associateBy { it.id }

                        // Insert/Update các bài hát từ API vào Database
                        val songEntitiesToInsert = apiSongs.map { apiSong ->
                            val existingEntity = entityMap[apiSong.id]
                            com.hust.musicdm.database.SongEntity(
                                id = apiSong.id,
                                title = apiSong.title ?: "Unknown",
                                artist = apiSong.artist ?: "Unknown Artist",
                                streamUrl = apiSong.streamUrl,
                                downloadUrl = apiSong.downloadUrl,
                                albumArt = apiSong.albumArt,
                                duration = apiSong.duration,
                                // Giữ nguyên trạng thái download nếu đã có
                                isDownloaded = existingEntity?.isDownloaded ?: false,
                                localPath = existingEntity?.localPath,
                                downloadedAt = existingEntity?.downloadedAt
                            )
                        }

                        // Insert vào database (REPLACE nếu đã tồn tại)
                        database.songDao().insertSongs(songEntitiesToInsert)

                        // Merge với trạng thái download
                        val mergedSongs = apiSongs.map { apiSong ->
                            val entity = entityMap[apiSong.id]
                            apiSong.copy(
                                isDownloaded = entity?.isDownloaded ?: false,
                                localPath = entity?.localPath
                            )
                        }

                        withContext(Dispatchers.Main) {
                            fullSongList = mergedSongs

                            // Cập nhật UI theo mode hiện tại
                            if (isOfflineMode) {
                                filterListSongsDownloaded()
                            } else {
                                songAdapter.submitList(mergedSongs)
                            }
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }
                }
            } catch (e: Exception) {
                // SỬA: Khi offline, load bài hát đã tải từ Database
                loadSongsFromDatabase()
                Toast.makeText(
                    this@MainActivity,
                    "Không có kết nối. Hiển thị bài hát đã tải.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // THÊM: Method load bài hát từ Database khi offline
    private fun loadSongsFromDatabase() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val entities = database.songDao().getAllSongsSync()

                // Convert SongEntity sang Song model
                val songsFromDb = entities.map { entity ->
                    Song(
                        id = entity.id,
                        title = entity.title,
                        artist = entity.artist,
                        streamUrl = entity.streamUrl,
                        downloadUrl = entity.downloadUrl,
                        albumArt = entity.albumArt,
                        duration = entity.duration,
                        isDownloaded = entity.isDownloaded,
                        localPath = entity.localPath
                    )
                }

                withContext(Dispatchers.Main) {
                    fullSongList = songsFromDb
                    filterListSongsDownloaded()
                    songAdapter.submitList(songsFromDb)
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun startDownload(song: Song) {
        lifecycleScope.launch {
            downloadManager.downloadSong(song.id, song.downloadUrl, "${song.title}.mp3")
                .collect { state ->
                    when (state) {
                        is DownloadManager.DownloadState.Downloading -> {
                            downloadProgressMap[song.id] = state.progress
                            songAdapter.updateDownloadProgress(downloadProgressMap)
                        }
                        is DownloadManager.DownloadState.Success -> {
                            downloadProgressMap.remove(song.id)
                            songAdapter.updateDownloadProgress(downloadProgressMap)

                            // Cập nhật Database
                            withContext(Dispatchers.IO) {
                                database.songDao().updateDownloadStatus(
                                    song.id,
                                    true,
                                    state.filePath,
                                    System.currentTimeMillis()
                                )
                            }

                            // SỬA: Cập nhật fullSongList ngay lập tức
                            fullSongList = fullSongList.map {
                                if (it.id == song.id) {
                                    it.copy(isDownloaded = true, localPath = state.filePath)
                                } else {
                                    it
                                }
                            }

                            // Cập nhật UI
                            if (isOfflineMode) {
                                filterListSongsDownloaded()
                            } else {
                                songAdapter.submitList(fullSongList)
                            }

                            Toast.makeText(this@MainActivity, "Đã tải: ${song.title}", Toast.LENGTH_SHORT).show()
                        }
                        is DownloadManager.DownloadState.Error -> {
                            downloadProgressMap.remove(song.id)
                            songAdapter.updateDownloadProgress(downloadProgressMap)
                            Toast.makeText(this@MainActivity, "Lỗi tải xuống", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
        }
    }

    // THÊM: Dialog xác nhận xóa
    private fun showDeleteConfirmDialog(song: Song) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc muốn xóa \"${song.title}\" khỏi thiết bị?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteSong(song)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // THÊM: Logic xóa bài hát
    private fun deleteSong(song: Song) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Xóa file
                val deleted = downloadManager.deleteDownloadedSong(song.id)

                if (deleted) {
                    // Cập nhật Database
                    database.songDao().updateDownloadStatus(
                        song.id,
                        false,
                        null,
                        null
                    )

                    withContext(Dispatchers.Main) {
                        // Cập nhật fullSongList
                        fullSongList = fullSongList.map {
                            if (it.id == song.id) {
                                it.copy(isDownloaded = false, localPath = null)
                            } else {
                                it
                            }
                        }

                        // Cập nhật UI
                        if (isOfflineMode) {
                            filterListSongsDownloaded()
                        } else {
                            songAdapter.submitList(fullSongList)
                        }

                        Toast.makeText(this@MainActivity, "Đã xóa: ${song.title}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Không thể xóa file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}