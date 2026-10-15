/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.kotlin.collection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.drawableColor
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.rangemanager.LithoCollectionItem
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.RangeManager
import com.meta.rangemanager.list.ListCollectionItemProvider
import com.meta.rangemanager.list.ListPosition
import com.meta.rangemanager.list.ListRangeConfig
import com.meta.rangemanager.list.bindToRecyclerView
import com.meta.rangemanager.ui.BindableCollectionItem

// region Constants

private const val VIEW_TYPE_TEXT = 0
private const val VIEW_TYPE_IMAGE = 1
private const val VIEW_TYPE_FOLLOW = 2

private const val PAGE_SIZE = 15
private const val INITIAL_LOAD_DELAY_MS = 1500L
private const val PAGE_LOAD_DELAY_MS = 1000L
private const val REFRESH_DELAY_MS = 1200L
private const val PAGINATION_THRESHOLD = 5

// endregion

// region Data Models

/** Base class for all feed items. */
sealed class FeedItem(val id: String, val viewType: Int)

/** A text-only post with like support. */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class TextPost(
    val postId: String,
    val authorName: String,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val timestamp: String,
) : FeedItem(id = postId, viewType = VIEW_TYPE_TEXT)

/** An image post (simulated with colored rectangle) with like support. */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class ImagePost(
    val postId: String,
    val authorName: String,
    val caption: String,
    val imageColor: Int,
    val likeCount: Int,
    val isLiked: Boolean,
    val timestamp: String,
) : FeedItem(id = postId, viewType = VIEW_TYPE_IMAGE)

/**
 * A follow suggestion card.
 *
 * Uses a custom content key: only re-prepares when [userId] or [isFollowing] changes.
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class FollowSuggestion(
    val userId: String,
    val userName: String,
    val userColor: Int,
    val isFollowing: Boolean,
) : FeedItem(id = userId, viewType = VIEW_TYPE_FOLLOW)

// endregion

// region Data Generation

/** Generates fake feed data for the sample. */
internal object FeedDataGenerator {

  private val authorNames = listOf(
      "Alice Chen",
      "Bob Martinez",
      "Carol Singh",
      "David Kim",
      "Elena Petrova",
      "Frank Okafor",
      "Grace Liu",
      "Hassan Ali",
      "Iris Tanaka",
      "Jake Wilson",
  )

  private val postTexts = listOf(
      "Just had the best coffee of my life. Sometimes it's the little things.",
      "Working on a new project and feeling really excited about the direction it's going!",
      "Anyone else watching the sunset right now? Absolutely stunning.",
      "Hot take: tabs are better than spaces. Fight me.",
      "Finally finished that book I've been reading for months. Highly recommend it!",
      "Learning Kotlin has been one of the best decisions I've made this year.",
      "The weather today is perfect for a long walk. Who's joining?",
      "Just shipped a feature I've been working on for weeks. Feels great!",
      "Reminder: drink water, stretch, and take breaks. Your future self will thank you.",
      "Can't believe it's already February. Time flies!",
  )

  private val captions = listOf(
      "Beautiful day out there",
      "New setup, who dis?",
      "Weekend vibes",
      "No filter needed",
      "Making memories",
      "Adventures await",
      "Good times with great people",
      "Living my best life",
  )

  private val timestamps =
      listOf("2m ago", "5m ago", "15m ago", "1h ago", "2h ago", "3h ago", "5h ago", "8h ago")

  private val imageColors = listOf(
      0xFF1565C0.toInt(),
      0xFF2E7D32.toInt(),
      0xFFC62828.toInt(),
      0xFF6A1B9A.toInt(),
      0xFFEF6C00.toInt(),
      0xFF00838F.toInt(),
      0xFF4E342E.toInt(),
      0xFF37474F.toInt(),
  )

  private val avatarColors = listOf(
      0xFFE91E63.toInt(),
      0xFF9C27B0.toInt(),
      0xFF3F51B5.toInt(),
      0xFF009688.toInt(),
      0xFFFF5722.toInt(),
      0xFF607D8B.toInt(),
  )

  private var nextId = 0

  /** Generates a page of feed items. Follow suggestions appear roughly every 5-7 items. */
  fun generatePage(pageIndex: Int): List<FeedItem> {
    val items = mutableListOf<FeedItem>()
    val random = java.util.Random(pageIndex.toLong() * 31 + nextId)

    repeat(PAGE_SIZE) {
      val globalIndex = nextId++
      val shouldInsertFollowSuggestion = (globalIndex % 6 == 4)

      if (shouldInsertFollowSuggestion) {
        items.add(
            FollowSuggestion(
                userId = "user_$globalIndex",
                userName = authorNames[random.nextInt(authorNames.size)],
                userColor = avatarColors[random.nextInt(avatarColors.size)],
                isFollowing = false,
            ),
        )
      } else if (random.nextBoolean()) {
        items.add(
            ImagePost(
                postId = "post_$globalIndex",
                authorName = authorNames[random.nextInt(authorNames.size)],
                caption = captions[random.nextInt(captions.size)],
                imageColor = imageColors[random.nextInt(imageColors.size)],
                likeCount = random.nextInt(500),
                isLiked = false,
                timestamp = timestamps[random.nextInt(timestamps.size)],
            ),
        )
      } else {
        items.add(
            TextPost(
                postId = "post_$globalIndex",
                authorName = authorNames[random.nextInt(authorNames.size)],
                text = postTexts[random.nextInt(postTexts.size)],
                likeCount = random.nextInt(200),
                isLiked = false,
                timestamp = timestamps[random.nextInt(timestamps.size)],
            ),
        )
      }
    }
    return items
  }

  /** Resets the ID counter (used on pull-to-refresh). */
  fun reset() {
    nextId = 0
  }
}

// endregion

// region Components

/** A circular drawable used to simulate user avatars. */
internal class CircleDrawable(private val circleColor: Int, private val sizePx: Int) : Drawable() {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = circleColor }

  override fun draw(canvas: Canvas) {
    val cx = bounds.exactCenterX()
    val cy = bounds.exactCenterY()
    val radius = minOf(bounds.width(), bounds.height()) / 2f
    canvas.drawCircle(cx, cy, radius, paint)
  }

  override fun getIntrinsicWidth(): Int = sizePx

  override fun getIntrinsicHeight(): Int = sizePx

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
  }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Displays a text-only post with author, body text, and a like button. */
internal class TextPostComponent(
    private val item: TextPost,
    private val onLikeToggle: (String) -> Unit,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(style = Style.padding(all = 16.dp).backgroundColor(Color.WHITE)) {
      child(
          Text(
              text = item.authorName,
              textSize = 16.sp,
              textColor = Color.BLACK,
              style = Style.padding(bottom = 4.dp),
          ),
      )
      child(
          Text(
              text = item.text,
              textSize = 14.sp,
              textColor = Color.DKGRAY,
              style = Style.padding(bottom = 8.dp),
          ),
      )
      child(
          Row(alignItems = YogaAlign.CENTER) {
            child(
                Text(
                    text = if (item.isLiked) "\u2665" else "\u2661",
                    textSize = 20.sp,
                    textColor = if (item.isLiked) Color.RED else Color.GRAY,
                    style = Style.onClick { onLikeToggle(item.postId) }.padding(end = 8.dp),
                ),
            )
            child(
                Text(
                    text = "${item.likeCount} likes",
                    textSize = 12.sp,
                    textColor = Color.GRAY,
                ),
            )
            child(
                Text(
                    text = item.timestamp,
                    textSize = 12.sp,
                    textColor = Color.LTGRAY,
                    style = Style.margin(start = 16.dp),
                ),
            )
          },
      )
    }
  }
}

/**
 * Displays an image post with a colored rectangle simulating an image, caption, and like button.
 */
internal class ImagePostComponent(
    private val item: ImagePost,
    private val onLikeToggle: (String) -> Unit,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(style = Style.backgroundColor(Color.WHITE)) {
      child(
          Text(
              text = item.authorName,
              textSize = 16.sp,
              textColor = Color.BLACK,
              style = Style.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
          ),
      )
      // Simulated image as a colored rectangle
      child(Column(style = Style.height(200.dp).background(drawableColor(item.imageColor))) {})
      child(
          Text(
              text = item.caption,
              textSize = 14.sp,
              textColor = Color.DKGRAY,
              style = Style.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
          ),
      )
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              style = Style.padding(start = 16.dp, bottom = 16.dp),
          ) {
            child(
                Text(
                    text = if (item.isLiked) "\u2665" else "\u2661",
                    textSize = 20.sp,
                    textColor = if (item.isLiked) Color.RED else Color.GRAY,
                    style = Style.onClick { onLikeToggle(item.postId) }.padding(end = 8.dp),
                ),
            )
            child(
                Text(
                    text = "${item.likeCount} likes",
                    textSize = 12.sp,
                    textColor = Color.GRAY,
                ),
            )
            child(
                Text(
                    text = item.timestamp,
                    textSize = 12.sp,
                    textColor = Color.LTGRAY,
                    style = Style.margin(start = 16.dp),
                ),
            )
          },
      )
    }
  }
}

/**
 * Displays a follow suggestion card with avatar circle, name, and follow/unfollow button.
 *
 * This component is used with a custom content key in the [FeedItemProvider] - it only re-prepares
 * when [FollowSuggestion.userId] or [FollowSuggestion.isFollowing] changes.
 */
internal class FollowSuggestionComponent(
    private val item: FollowSuggestion,
    private val onFollowToggle: (String) -> Unit,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val density = context.androidContext.resources.displayMetrics.density
    val avatarSizePx = (40 * density).toInt()

    return Row(
        alignItems = YogaAlign.CENTER,
        style = Style.padding(all = 16.dp).backgroundColor(0xFFF5F5F5.toInt()),
    ) {
      // Avatar circle
      child(
          Column(
              style =
                  Style.width(40.dp)
                      .height(40.dp)
                      .background(CircleDrawable(item.userColor, avatarSizePx))
                      .margin(end = 12.dp),
          ) {},
      )
      // User name
      child(
          Text(
              text = item.userName,
              textSize = 15.sp,
              textColor = Color.BLACK,
              style = Style.flex(grow = 1f),
          ),
      )
      // Follow/Following button
      child(
          Text(
              text = if (item.isFollowing) "Following" else "Follow",
              textSize = 14.sp,
              textColor = if (item.isFollowing) Color.GRAY else Color.WHITE,
              style =
                  Style.onClick { onFollowToggle(item.userId) }
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .background(
                          drawableColor(
                              if (item.isFollowing) 0xFFE0E0E0.toInt() else 0xFF1976D2.toInt(),
                          ),
                      ),
          ),
      )
    }
  }
}

// endregion

// region Item Provider & Adapter

/**
 * Provides [LithoCollectionItem] instances for each feed position.
 *
 * The [FollowSuggestion] type uses a custom content key that only triggers re-preparation when the
 * user ID or follow status changes, skipping updates for other field changes.
 */
internal class FeedItemProvider(
    private val context: Context,
    private val items: () -> List<FeedItem>,
    private val onLikeToggle: (String) -> Unit,
    private val onFollowToggle: (String) -> Unit,
) : ListCollectionItemProvider() {

  private val sizeConstraints = SizeConstraints(
      minWidth = 0,
      maxWidth = context.resources.displayMetrics.widthPixels,
      minHeight = 0,
      maxHeight = SizeConstraints.Infinity,
  )

  override val size: Int
    get() = items().size

  fun getViewType(position: Int): Int = items()[position].viewType

  override fun create(key: Int): PreparableCollectionItem {
    val item = items()[key]
    return when (item) {
      is TextPost ->
          LithoCollectionItem(
              context = context,
              dataProvider = { items()[key] as TextPost },
              component = { data -> TextPostComponent(item = data, onLikeToggle = onLikeToggle) },
              sizeConstraints = sizeConstraints,
              traceTag = "TextPost",
          )
      is ImagePost ->
          LithoCollectionItem(
              context = context,
              dataProvider = { items()[key] as ImagePost },
              component = { data -> ImagePostComponent(item = data, onLikeToggle = onLikeToggle) },
              sizeConstraints = sizeConstraints,
              traceTag = "ImagePost",
          )
      is FollowSuggestion ->
          LithoCollectionItem(
              context = context,
              dataProvider = { items()[key] as FollowSuggestion },
              component = { data ->
                FollowSuggestionComponent(item = data, onFollowToggle = onFollowToggle)
              },
              sizeConstraints = sizeConstraints,
              traceTag = "FollowSuggestion",
              // Only re-prepare when userId or isFollowing changes
              contentKey = { data -> data.userId to data.isFollowing },
          )
    }
  }
}

/**
 * RecyclerView adapter with 3 view types for the social feed.
 *
 * Each view type gets its own RecyclerView view pool for efficient recycling.
 */
private class FeedRecyclerAdapter(private val itemProvider: FeedItemProvider) :
    RecyclerView.Adapter<FeedLithoViewHolder>() {

  override fun getItemViewType(position: Int): Int = itemProvider.getViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedLithoViewHolder {
    val lithoView = LithoView(parent.context)
    lithoView.layoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    return FeedLithoViewHolder(lithoView)
  }

  override fun onBindViewHolder(holder: FeedLithoViewHolder, position: Int) {
    (itemProvider[position] as BindableCollectionItem).bind(holder.lithoView)
  }

  override fun onViewRecycled(holder: FeedLithoViewHolder) {
    val position = holder.bindingAdapterPosition
    if (position != RecyclerView.NO_POSITION) {
      (itemProvider[position] as BindableCollectionItem).unbind()
    }
  }

  override fun getItemCount(): Int = itemProvider.size
}

private class FeedLithoViewHolder(val lithoView: LithoView) : RecyclerView.ViewHolder(lithoView)

// endregion

// region Activity

/**
 * A sample Activity demonstrating RangeManager with a realistic social feed.
 *
 * This example shows:
 * 1. Three different view types (TextPost, ImagePost, FollowSuggestion) with separate view pools
 * 2. Custom content key on FollowSuggestion to skip unnecessary re-preparation
 * 3. Simulated initial loading with spinner
 * 4. Pagination when scrolling near the end
 * 5. Pull-to-refresh to reload the feed
 * 6. In-place like and follow/unfollow interactions that update via RangeManager
 */
class RangeManagerFeedActivity : AppCompatActivity() {

  private val feedItems = mutableListOf<FeedItem>()
  private var currentPage = 0
  private var isLoadingMore = false

  private var rangeManager: RangeManager<ListPosition, IntRange>? = null
  private lateinit var itemProvider: FeedItemProvider
  private lateinit var adapter: FeedRecyclerAdapter
  private lateinit var layoutManager: LinearLayoutManager

  private val mainHandler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    itemProvider =
        FeedItemProvider(
            context = this,
            items = { feedItems },
            onLikeToggle = ::onLikeToggle,
            onFollowToggle = ::onFollowToggle,
        )

    adapter = FeedRecyclerAdapter(itemProvider)
    layoutManager = LinearLayoutManager(this)

    val recyclerView = RecyclerView(this)
    recyclerView.layoutManager = layoutManager
    recyclerView.adapter = adapter

    // RangeManager: scroll events wired automatically; destroy() called on ON_DESTROY.
    rangeManager =
        RangeManager.bindToRecyclerView(
            itemProvider = itemProvider,
            recyclerView = recyclerView,
            config = ListRangeConfig(behind = 5, ahead = 10),
            lifecycleOwner = this,
        )

    // Disable change animations so notifyItemChanged rebinds in place instead of
    // creating a new ViewHolder. This avoids a race where the old ViewHolder's
    // onViewRecycled unbinds the LithoCollectionItem that was already re-bound to
    // the new ViewHolder, causing the component to disappear.
    (recyclerView.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false

    // Divider: thin gray line between items
    recyclerView.addItemDecoration(
        object : RecyclerView.ItemDecoration() {
          override fun getItemOffsets(
              outRect: android.graphics.Rect,
              view: View,
              parent: RecyclerView,
              state: RecyclerView.State,
          ) {
            outRect.bottom = (1 * resources.displayMetrics.density).toInt()
          }
        },
    )

    // Scroll listener for pagination only (range recalculation handled by RangeManager)
    recyclerView.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
          override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            if (!isLoadingMore && lastVisible >= feedItems.size - PAGINATION_THRESHOLD) {
              loadMoreItems()
            }
          }
        },
    )

    // Pull-to-refresh
    val swipeRefreshLayout = SwipeRefreshLayout(this)
    swipeRefreshLayout.addView(recyclerView)
    swipeRefreshLayout.setOnRefreshListener { refreshFeed(swipeRefreshLayout) }

    // Loading overlay
    val loadingOverlay = FrameLayout(this)
    loadingOverlay.setBackgroundColor(Color.WHITE)
    val progressBar = ProgressBar(this)
    val progressParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
    progressParams.gravity = Gravity.CENTER
    loadingOverlay.addView(progressBar, progressParams)

    // Root layout
    val root = FrameLayout(this)
    root.addView(
        swipeRefreshLayout,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
    root.addView(
        loadingOverlay,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    setContentView(root)

    // Simulate initial load
    mainHandler.postDelayed(
        {
          val firstPage = FeedDataGenerator.generatePage(currentPage++)
          feedItems.addAll(firstPage)
          adapter.notifyItemRangeInserted(0, firstPage.size)
          loadingOverlay.visibility = View.GONE
        },
        INITIAL_LOAD_DELAY_MS,
    )
  }

  private fun onLikeToggle(postId: String) {
    val index = feedItems.indexOfFirst { it.id == postId }
    if (index == -1) return

    val item = feedItems[index]
    val updatedItem =
        when (item) {
          is TextPost ->
              item.copy(
                  isLiked = !item.isLiked,
                  likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1,
              )
          is ImagePost ->
              item.copy(
                  isLiked = !item.isLiked,
                  likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1,
              )
          is FollowSuggestion -> return // Follow suggestions don't have likes
        }

    feedItems[index] = updatedItem
    adapter.notifyItemChanged(index)
    recalculateCurrentRange()
  }

  private fun onFollowToggle(userId: String) {
    val index = feedItems.indexOfFirst { it.id == userId }
    if (index == -1) return

    val item = feedItems[index] as? FollowSuggestion ?: return
    feedItems[index] = item.copy(isFollowing = !item.isFollowing)
    adapter.notifyItemChanged(index)
    recalculateCurrentRange()
  }

  private fun refreshFeed(swipeRefreshLayout: SwipeRefreshLayout) {
    mainHandler.postDelayed(
        {
          FeedDataGenerator.reset()
          currentPage = 0
          val oldSize = feedItems.size
          feedItems.clear()
          itemProvider.invalidateAll()
          adapter.notifyItemRangeRemoved(0, oldSize)

          val newPage = FeedDataGenerator.generatePage(currentPage++)
          feedItems.addAll(newPage)
          adapter.notifyItemRangeInserted(0, newPage.size)
          swipeRefreshLayout.isRefreshing = false
        },
        REFRESH_DELAY_MS,
    )
  }

  private fun loadMoreItems() {
    isLoadingMore = true
    mainHandler.postDelayed(
        {
          val insertStart = feedItems.size
          val newPage = FeedDataGenerator.generatePage(currentPage++)
          feedItems.addAll(newPage)
          adapter.notifyItemRangeInserted(insertStart, newPage.size)
          recalculateCurrentRange()
          isLoadingMore = false
        },
        PAGE_LOAD_DELAY_MS,
    )
  }

  private fun recalculateCurrentRange() {
    val firstVisible = layoutManager.findFirstVisibleItemPosition()
    val lastVisible = layoutManager.findLastVisibleItemPosition()
    if (firstVisible != RecyclerView.NO_POSITION) {
      rangeManager?.recalculateRange(
          ListPosition(currentFirstVisible = firstVisible, currentLastVisible = lastVisible),
      )
    }
  }
}

// endregion
