package com.wpi.openspot.ui.lots

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wpi.openspot.R
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LotsFragment : Fragment(R.layout.fragment_lots) {

    private val viewModel: LotsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvLots = view.findViewById<RecyclerView>(R.id.rvLots)
        val adapter = LotAdapter()
        rvLots.adapter = adapter
        rvLots.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lots.collect { lots ->
                adapter.submitList(lots)
            }
        }
    }
}

class LotAdapter : RecyclerView.Adapter<LotAdapter.LotViewHolder>() {

    private var lots = listOf<ParkingLot>()

    fun submitList(newLots: List<ParkingLot>) {
        lots = newLots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LotViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lot, parent, false)
        return LotViewHolder(view)
    }

    override fun onBindViewHolder(holder: LotViewHolder, position: Int) {
        holder.bind(lots[position])
    }

    override fun getItemCount() = lots.size

    class LotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(lot: ParkingLot) {
            val tvLotName = itemView.findViewById<TextView>(R.id.tvLotName)
            val tvOccupancy = itemView.findViewById<TextView>(R.id.tvOccupancy)
            val tvPermitTypes = itemView.findViewById<TextView>(R.id.tvPermitTypes)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val tvLastUpdated = itemView.findViewById<TextView>(R.id.tvLastUpdated)
            val statusIndicator = itemView.findViewById<View>(R.id.statusIndicator)
            val occupancyProgress = itemView.findViewById<ProgressBar>(R.id.occupancyProgress)

            tvLotName.text = lot.name

            val percent = if (lot.capacity > 0)
                (lot.occupancy * 100) / lot.capacity else 0
            tvOccupancy.text = "${lot.occupancy} / ${lot.capacity} spaces"
            occupancyProgress.progress = percent
            tvPermitTypes.text = "Permits: ${lot.permitTypes.joinToString(", ")}"
            tvStatus.text = lot.status.name.replace("_", " ")

            if (lot.lastUpdatedAt > 0) {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                tvLastUpdated.text = sdf.format(Date(lot.lastUpdatedAt))
            } else {
                tvLastUpdated.text = ""
            }

            val (statusColor, badgeColor, textColor) = when (lot.status) {
                LotStatus.AVAILABLE   -> Triple("#4CAF50", "#E8F5E9", "#2E7D32")
                LotStatus.ALMOST_FULL -> Triple("#FF9800", "#FFF3E0", "#E65100")
                LotStatus.FULL        -> Triple("#F44336", "#FFEBEE", "#C62828")
                LotStatus.UNKNOWN     -> Triple("#9E9E9E", "#F5F5F5", "#424242")
            }

            statusIndicator.background.setTint(Color.parseColor(statusColor))
            occupancyProgress.progressTintList = android.content.res.ColorStateList
                .valueOf(Color.parseColor(statusColor))
            tvStatus.setTextColor(Color.parseColor(textColor))
            tvStatus.background.setTint(Color.parseColor(badgeColor))
        }
    }
}
