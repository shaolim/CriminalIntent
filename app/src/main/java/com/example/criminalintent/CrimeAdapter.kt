package com.example.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.dto.CrimeViewType
import com.example.criminalintent.dto.ViewType
import com.example.criminalintent.entity.Crime
import com.example.criminalintent.util.formatDate
import kotlinx.android.synthetic.main.item_crime.view.*
import kotlinx.android.synthetic.main.item_crime_header.view.*
import java.text.SimpleDateFormat
import java.util.*

class CrimeAdapter(private val listener: CrimeListFragment.Callbacks?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val crimes: MutableList<CrimeViewType> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HEADER.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_crime_header, parent, false)
                HeaderHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_crime, parent, false)
                ItemHolder(view)
            }
        }
    }

    override fun getItemCount(): Int = crimes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val crime = crimes[position]

        when (getItemViewType(position)) {
            ViewType.HEADER.ordinal -> {
                (holder as HeaderHolder).bind(crime.header)
            }
            else -> {
                (holder as ItemHolder).bind(crime.crime)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return crimes[position].type.ordinal
    }

    fun submitList(newCrimeList: List<Crime>) {
        val newList = generateList(newCrimeList)
        val diffCallback = DiffCallback(crimes, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        crimes.clear()
        crimes.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun generateList(crimes: List<Crime>): List<CrimeViewType> {
        val header = crimes.map { crime ->
            return@map getDateFromDateTime(crime.date)
        }.distinct()

        return mutableListOf<CrimeViewType>().apply {
            var headerIndex = 0

            crimes.forEach {
                val date = getDateFromDateTime(it.date)
                if (headerIndex < header.size && date?.compareTo(header[headerIndex]) == 0) {
                    add(CrimeViewType(ViewType.HEADER, header = header[headerIndex]))
                    headerIndex += 1
                }

                add(CrimeViewType(ViewType.ITEM, crime = it))
            }
        }
    }

    private fun getDateFromDateTime(date: Date): Date? {
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        return formatter.parse(formatter.format(date))
    }

    inner class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(date: Date?) {
            date?.let {
                itemView.txvCrimeHeader.text = formatDate(it, itemView.resources)
            }
        }
    }

    inner class ItemHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var crime: Crime

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime?) {
            crime?.let {
                this.crime = it
                itemView.txvCrimeTitle.text = it.title
                itemView.txvCrimeDate.text =
                    formatDate(
                        it.date,
                        itemView.resources
                    )
                itemView.ivSolved.visibility = if (it.isSolved) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        override fun onClick(v: View?) {
            listener?.onCrimeSelected(crime.id)
        }
    }

    inner class DiffCallback(
        private val oldList: List<CrimeViewType>,
        private val newList: List<CrimeViewType>
    ) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].type == newList[newItemPosition].type
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

    }
}