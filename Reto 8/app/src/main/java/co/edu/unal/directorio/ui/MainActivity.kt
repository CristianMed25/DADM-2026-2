package co.edu.unal.directorio.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import co.edu.unal.directorio.R
import co.edu.unal.directorio.data.Classification
import co.edu.unal.directorio.data.CompanyDbHelper
import co.edu.unal.directorio.data.CompanyRepository
import co.edu.unal.directorio.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: CompanyRepository
    private lateinit var adapter: CompanyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CompanyRepository(CompanyDbHelper(this))
        adapter = CompanyAdapter { company ->
            startActivity(Intent(this, CompanyDetailActivity::class.java).apply {
                putExtra(CompanyDetailActivity.EXTRA_COMPANY_ID, company.id)
            })
        }
        binding.recyclerCompanies.adapter = adapter

        binding.editSearchName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshList()
        })

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ -> refreshList() }

        binding.fabAddCompany.setOnClickListener {
            startActivity(Intent(this, CompanyFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun selectedClassification(): Classification? = when (binding.chipGroupFilter.checkedChipId) {
        R.id.chipConsulting -> Classification.CONSULTING
        R.id.chipCustomDevelopment -> Classification.CUSTOM_DEVELOPMENT
        R.id.chipSoftwareFactory -> Classification.SOFTWARE_FACTORY
        else -> null
    }

    private fun refreshList() {
        val companies = repository.listCompanies(
            nameFilter = binding.editSearchName.text?.toString().orEmpty(),
            classificationFilter = selectedClassification()
        )
        adapter.submitList(companies)
        binding.emptyState.visibility = if (companies.isEmpty()) View.VISIBLE else View.GONE
    }
}
