package co.edu.unal.directorio.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.directorio.R
import co.edu.unal.directorio.data.Company
import co.edu.unal.directorio.databinding.ItemCompanyBinding

class CompanyAdapter(
    private val onCompanyClicked: (Company) -> Unit
) : ListAdapter<Company, CompanyAdapter.CompanyViewHolder>(DIFF) {

    private var monogramColors: IntArray = intArrayOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        if (monogramColors.isEmpty()) {
            monogramColors = parent.context.resources.getIntArray(R.array.monogram_colors)
        }
        val binding = ItemCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompanyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompanyViewHolder(private val binding: ItemCompanyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(company: Company) {
            binding.textMonogram.text =
                company.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.textMonogram.background.setTint(
                monogramColors[(company.id % monogramColors.size).toInt()]
            )
            binding.textName.text = company.name
            binding.textClassifications.text =
                company.classifications.joinToString(" • ") { it.displayName }
            if (company.phone.isNotBlank()) {
                binding.textPhone.text = company.phone
                binding.rowPhone.visibility = View.VISIBLE
            } else {
                binding.rowPhone.visibility = View.GONE
            }
            itemView.setOnClickListener { onCompanyClicked(company) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Company>() {
            override fun areItemsTheSame(oldItem: Company, newItem: Company): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Company, newItem: Company): Boolean =
                oldItem == newItem
        }
    }
}
