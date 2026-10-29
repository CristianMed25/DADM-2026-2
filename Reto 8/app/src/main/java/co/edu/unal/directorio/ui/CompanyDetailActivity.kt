package co.edu.unal.directorio.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import co.edu.unal.directorio.R
import co.edu.unal.directorio.data.Company
import co.edu.unal.directorio.data.CompanyDbHelper
import co.edu.unal.directorio.data.CompanyRepository
import co.edu.unal.directorio.databinding.ActivityCompanyDetailBinding
import com.google.android.material.chip.Chip

class CompanyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyDetailBinding
    private lateinit var repository: CompanyRepository
    private var companyId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyId = intent.getLongExtra(EXTRA_COMPANY_ID, 0L)
        repository = CompanyRepository(CompanyDbHelper(this))

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonEdit.setOnClickListener {
            startActivity(Intent(this, CompanyFormActivity::class.java).apply {
                putExtra(CompanyFormActivity.EXTRA_COMPANY_ID, companyId)
            })
        }
        binding.buttonDelete.setOnClickListener { showDeleteConfirmation() }
    }

    override fun onResume() {
        super.onResume()
        loadCompany()
    }

    private fun loadCompany() {
        val company = repository.getCompany(companyId)
        if (company == null) {
            Toast.makeText(this, R.string.error_company_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        bindHeader(company)
        binding.textUrl.text = company.url.ifBlank { getString(R.string.not_registered) }
        binding.textPhone.text = company.phone.ifBlank { getString(R.string.not_registered) }
        binding.textEmail.text = company.email.ifBlank { getString(R.string.not_registered) }
        binding.textProducts.text = company.products.ifBlank { getString(R.string.not_registered) }
    }

    private fun bindHeader(company: Company) {
        binding.textMonogram.text =
            company.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        binding.textMonogram.background.setTint(
            monogramColors[(company.id % monogramColors.size).toInt()]
        )
        binding.textName.text = company.name

        binding.chipGroupClassifications.removeAllViews()
        company.classifications.forEach { classification ->
            binding.chipGroupClassifications.addView(
                Chip(this).apply { text = classification.displayName }
            )
        }
    }

    private fun showDeleteConfirmation() {
        val company = repository.getCompany(companyId) ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_company)
            .setMessage(getString(R.string.delete_confirmation, company.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                if (repository.deleteCompany(companyId)) {
                    Toast.makeText(this, R.string.company_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.error_delete_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private val monogramColors: IntArray by lazy {
        resources.getIntArray(R.array.monogram_colors)
    }

    companion object {
        const val EXTRA_COMPANY_ID = "extra_company_id"
    }
}
