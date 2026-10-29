package co.edu.unal.directorio.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.edu.unal.directorio.R
import co.edu.unal.directorio.data.Classification
import co.edu.unal.directorio.data.Company
import co.edu.unal.directorio.data.CompanyDbHelper
import co.edu.unal.directorio.data.CompanyRepository
import co.edu.unal.directorio.data.CompanyValidator
import co.edu.unal.directorio.databinding.ActivityCompanyFormBinding

class CompanyFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyFormBinding
    private lateinit var repository: CompanyRepository
    private var companyId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyId = intent.getLongExtra(EXTRA_COMPANY_ID, 0L)
        repository = CompanyRepository(CompanyDbHelper(this))

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setTitle(
            if (companyId != 0L) R.string.edit_company else R.string.add_company
        )

        if (companyId != 0L) {
            loadCompany()
        }

        binding.buttonSave.setOnClickListener { save() }
    }

    private fun loadCompany() {
        val company = repository.getCompany(companyId)
        if (company == null) {
            Toast.makeText(this, R.string.error_company_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.editName.setText(company.name)
        binding.editUrl.setText(company.url)
        binding.editPhone.setText(company.phone)
        binding.editEmail.setText(company.email)
        binding.editProducts.setText(company.products)
        binding.chipConsultingOption.isChecked =
            company.classifications.contains(Classification.CONSULTING)
        binding.chipCustomDevelopmentOption.isChecked =
            company.classifications.contains(Classification.CUSTOM_DEVELOPMENT)
        binding.chipSoftwareFactoryOption.isChecked =
            company.classifications.contains(Classification.SOFTWARE_FACTORY)
    }

    private fun selectedClassifications(): List<Classification> {
        val checkedIds = binding.chipGroupClassifications.checkedChipIds
        return buildList {
            if (R.id.chipConsultingOption in checkedIds) add(Classification.CONSULTING)
            if (R.id.chipCustomDevelopmentOption in checkedIds) add(Classification.CUSTOM_DEVELOPMENT)
            if (R.id.chipSoftwareFactoryOption in checkedIds) add(Classification.SOFTWARE_FACTORY)
        }
    }

    private fun save() {
        val name = CompanyValidator.normalizeName(binding.editName.text?.toString().orEmpty())
        val url = binding.editUrl.text?.toString().orEmpty().trim()
        val phone = binding.editPhone.text?.toString().orEmpty().trim()
        val email = binding.editEmail.text?.toString().orEmpty().trim()
        val products = binding.editProducts.text?.toString().orEmpty().trim()
        val classifications = selectedClassifications()

        val errors = CompanyValidator.validate(name, url, phone, email, products, classifications.size)

        binding.inputName.error =
            if (CompanyValidator.Field.NAME in errors) getString(R.string.error_name_required) else null
        binding.inputUrl.error =
            if (CompanyValidator.Field.URL in errors) getString(R.string.error_url_invalid) else null
        binding.inputPhone.error =
            if (CompanyValidator.Field.PHONE in errors) getString(R.string.error_phone_invalid) else null
        binding.inputEmail.error =
            if (CompanyValidator.Field.EMAIL in errors) getString(R.string.error_email_invalid) else null
        binding.inputProducts.error =
            if (CompanyValidator.Field.PRODUCTS in errors) getString(R.string.error_products_required) else null
        binding.textClassificationError.visibility =
            if (CompanyValidator.Field.CLASSIFICATIONS in errors) View.VISIBLE else View.GONE

        if (errors.isNotEmpty()) return

        if (repository.nameExists(name, companyId)) {
            binding.inputName.error = getString(R.string.error_duplicate_name)
            return
        }

        val company = Company(
            id = companyId,
            name = name,
            url = url,
            phone = phone,
            email = email,
            products = products,
            classifications = classifications
        )

        if (companyId == 0L) {
            val newId = repository.insertCompany(company)
            if (newId == -1L) {
                Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(this, R.string.company_saved, Toast.LENGTH_SHORT).show()
        } else {
            if (!repository.updateCompany(company)) {
                Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(this, R.string.company_updated, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        const val EXTRA_COMPANY_ID = "extra_company_id"
    }
}
