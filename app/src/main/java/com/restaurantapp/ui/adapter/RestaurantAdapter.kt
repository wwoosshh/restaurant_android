package com.restaurantapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.restaurantapp.R
import com.restaurantapp.data.model.Restaurant
import com.restaurantapp.databinding.ItemRestaurantBinding

class RestaurantAdapter(
    private val onItemClick: (Restaurant) -> Unit,
    private val onBookmarkClick: (Restaurant) -> Unit,
    private val onFavoriteClick: (Restaurant) -> Unit
) : ListAdapter<Restaurant, RestaurantAdapter.RestaurantViewHolder>(RestaurantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val binding = ItemRestaurantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RestaurantViewHolder(binding, onItemClick, onBookmarkClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RestaurantViewHolder(
        private val binding: ItemRestaurantBinding,
        private val onItemClick: (Restaurant) -> Unit,
        private val onBookmarkClick: (Restaurant) -> Unit,
        private val onFavoriteClick: (Restaurant) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(restaurant: Restaurant) {
            // 기본 정보 설정
            binding.textViewName.text = restaurant.name
            binding.textViewCategory.text = restaurant.category
            binding.textViewAddress.text = restaurant.address
            binding.textViewRating.text = String.format("%.1f", restaurant.rating)
            binding.textViewReviewCount.text = "(${restaurant.reviewCount})"
            binding.textViewPriceRange.text = restaurant.priceRange ?: "보통"

            // 전화번호 표시 (있는 경우만)
            if (!restaurant.phone.isNullOrBlank()) {
                binding.textViewPhone.text = restaurant.phone
                binding.textViewPhone.visibility = View.VISIBLE
            } else {
                binding.textViewPhone.visibility = View.GONE
            }

            // 설명 표시 (있는 경우만)
            if (!restaurant.description.isNullOrBlank()) {
                binding.textViewDescription.text = restaurant.description
                binding.textViewDescription.visibility = View.VISIBLE
            } else {
                binding.textViewDescription.visibility = View.GONE
            }

            // 북마크 상태에 따른 아이콘 변경
            if (restaurant.isBookmarked) {
                binding.buttonBookmark.setImageResource(R.drawable.ic_bookmark_24)
                binding.buttonBookmark.setColorFilter(
                    binding.root.context.getColor(R.color.bookmark_active)
                )
            } else {
                binding.buttonBookmark.setImageResource(R.drawable.ic_bookmark_border_24)
                binding.buttonBookmark.setColorFilter(
                    binding.root.context.getColor(R.color.icon_default)
                )
            }

            // 즐겨찾기 상태에 따른 아이콘 변경
            if (restaurant.isFavorite) {
                binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_24)
                binding.buttonFavorite.setColorFilter(
                    binding.root.context.getColor(R.color.favorite_active)
                )
            } else {
                binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_border_24)
                binding.buttonFavorite.setColorFilter(
                    binding.root.context.getColor(R.color.icon_default)
                )
            }

            // 맛집 이미지 설정 (향후 Glide 등으로 로드)
            if (!restaurant.imageUrl.isNullOrBlank()) {
                // TODO: Glide나 Coil로 이미지 로드
                binding.imageViewRestaurant.setImageResource(R.drawable.ic_restaurant_24)
            } else {
                binding.imageViewRestaurant.setImageResource(R.drawable.ic_restaurant_24)
            }

            // 클릭 리스너 설정
            binding.root.setOnClickListener {
                onItemClick(restaurant)
            }

            binding.buttonBookmark.setOnClickListener {
                onBookmarkClick(restaurant)
            }

            binding.buttonFavorite.setOnClickListener {
                onFavoriteClick(restaurant)
            }

            // 카테고리에 따른 배경색 설정 (선택사항)
            val categoryBackgroundRes = when (restaurant.category) {
                "한식" -> R.drawable.bg_category_korean
                "중식" -> R.drawable.bg_category_chinese
                "일식" -> R.drawable.bg_category_japanese
                "양식" -> R.drawable.bg_category_western
                "카페" -> R.drawable.bg_category_cafe
                "디저트" -> R.drawable.bg_category_dessert
                "패스트푸드" -> R.drawable.bg_category_fastfood
                else -> R.drawable.bg_category_chip
            }
            binding.textViewCategory.setBackgroundResource(categoryBackgroundRes)

            // 평점에 따른 별 색상 변경
            val starColorRes = when {
                restaurant.rating >= 4.5 -> R.color.rating_excellent
                restaurant.rating >= 4.0 -> R.color.rating_good
                restaurant.rating >= 3.0 -> R.color.rating_average
                else -> R.color.rating_poor
            }
            // 별 아이콘 색상 적용은 item_restaurant.xml에서 처리
        }
    }

    class RestaurantDiffCallback : DiffUtil.ItemCallback<Restaurant>() {
        override fun areItemsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
            return oldItem == newItem
        }
    }
}