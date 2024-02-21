package com.hazel.speedometer.ui.history.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.hazel.speedometer.databinding.LayoutItemHistoryBinding
import com.hazel.speedometer.room.beans.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter (
    private val onTripSelection: () -> Unit,
) : ListAdapter<Trip, HistoryAdapter.ViewHolder>(DiffUtilCallBack) {
    var selectionMode = false
    val selectedItems = mutableListOf<Trip>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            LayoutItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if (payloads[0] == "Selected") {
                holder.updateTripUI(getItem(position))
            } else {
                holder.updateTripUI(getItem(position))
            }
        }
    }

    fun clearSelection() {
        selectionMode = false
        for (trip in selectedItems) {
            notifyItemChanged(currentList.indexOf(trip), "Unselected")
        }
        selectedItems.clear()
    }

    inner class ViewHolder(private val binding: LayoutItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bindData(trip: Trip) {
            val startLatLng = LatLng(
                trip.start.substringBefore(",").toDouble(),
                trip.start.substringAfter(",").toDouble()
            )
            val endLatLng = LatLng(
                trip.end.substringBefore(",").toDouble(),
                trip.end.substringAfter(",").toDouble()
            )
            binding.tvStart.text = getAddressFromLocation(startLatLng, binding.root.context)
            binding.tvEnd.text = getAddressFromLocation(endLatLng, binding.root.context)
            binding.tvDate.text = convertMillisToDate(trip.date)

            binding.root.setOnLongClickListener {
                if (!selectionMode) {
                    selectionMode = true
                    onTripSelection()
                }
                selectTrip(trip)
                return@setOnLongClickListener true
            }

            binding.root.setOnClickListener {
                if (selectionMode) {
                    selectTrip(trip)
                }
            }
        }

        private fun selectTrip(trip: Trip) {
            if (selectedItems.contains(trip)) {
                selectedItems.remove(trip)
                binding.root.post {
                    notifyItemChanged(adapterPosition, "Unselected")
                }
            } else {
                selectedItems.add(trip)
                binding.root.post {
                    notifyItemChanged(adapterPosition, "Selected")
                }
            }
            updateTripUI(trip)
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun updateTripUI(trip: Trip) {
            if (selectedItems.contains(trip)) {
                binding.root.setBackgroundColor(binding.root.context.getColor(android.R.color.holo_blue_light))
            } else {
                binding.root.setBackgroundColor(binding.root.context.getColor(android.R.color.transparent))
            }
        }

        @SuppressLint("SimpleDateFormat")
        private fun convertMillisToDate(millis: Long): String {
            val date = Date(millis)
            val format = SimpleDateFormat("dd/MM/yyyy")
            return format.format(date)
        }

        private fun getAddressFromLocation(lat: LatLng, context: Context): String {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address> = geocoder.getFromLocation(lat.latitude,lat.longitude, 1)!!
            return addresses[0].getAddressLine(0)
        }
    }

    object DiffUtilCallBack : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }
}