package co.edu.unal.directorio.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class CompanyRepository(private val dbHelper: CompanyDbHelper) {

    fun insertCompany(company: Company): Long {
        val db = dbHelper.writableDatabase
        val values = companyContentValues(company)
        val id = db.insertWithOnConflict("companies", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        if (id != -1L) {
            replaceClassifications(db, id, company.classifications)
        }
        return id
    }

    fun updateCompany(company: Company): Boolean {
        val db = dbHelper.writableDatabase
        val values = companyContentValues(company)
        val updated = db.update("companies", values, "id = ?", arrayOf(company.id.toString())) > 0
        if (updated) {
            replaceClassifications(db, company.id, company.classifications)
        }
        return updated
    }

    fun deleteCompany(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("companies", "id = ?", arrayOf(id.toString())) > 0
    }

    fun getCompany(id: Long): Company? {
        val db = dbHelper.readableDatabase
        db.query(TABLE_COMPANIES, null, "id = ?", arrayOf(id.toString()), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Company(
                id = cursor.getLong(COLUMN_ID),
                name = cursor.getString(COLUMN_NAME),
                url = cursor.getString(COLUMN_URL),
                phone = cursor.getString(COLUMN_PHONE),
                email = cursor.getString(COLUMN_EMAIL),
                products = cursor.getString(COLUMN_PRODUCTS),
                classifications = getClassificationsFor(db, id)
            )
        }
    }

    fun listCompanies(nameFilter: String = "", classificationFilter: Classification? = null): List<Company> {
        val db = dbHelper.readableDatabase
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        val name = nameFilter.trim()
        if (name.isNotEmpty()) {
            conditions += "$TABLE_COMPANIES.$COLUMN_NAME LIKE ?"
            args += "%$name%"
        }
        if (classificationFilter != null) {
            conditions += """
                EXISTS (
                    SELECT 1 FROM $TABLE_COMPANY_CLASSIFICATION cc
                    WHERE cc.company_id = $TABLE_COMPANIES.$COLUMN_ID AND cc.classification_id = ?
                )
            """.trimIndent().replace('\n', ' ')
            args += classificationFilter.id.toString()
        }

        val classificationsByCompany = getAllClassifications(db)
        val companies = mutableListOf<Company>()

        db.query(
            TABLE_COMPANIES,
            null,
            if (conditions.isEmpty()) null else conditions.joinToString(" AND "),
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            "$TABLE_COMPANIES.$COLUMN_NAME COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(COLUMN_ID)
                companies += Company(
                    id = id,
                    name = cursor.getString(COLUMN_NAME),
                    url = cursor.getString(COLUMN_URL),
                    phone = cursor.getString(COLUMN_PHONE),
                    email = cursor.getString(COLUMN_EMAIL),
                    products = cursor.getString(COLUMN_PRODUCTS),
                    classifications = classificationsByCompany[id].orEmpty()
                )
            }
        }
        return companies
    }

    fun nameExists(name: String, excludeId: Long = 0L): Boolean {
        val db = dbHelper.readableDatabase
        db.query(
            TABLE_COMPANIES,
            arrayOf(COLUMN_ID),
            "$COLUMN_NAME = ? AND $COLUMN_ID != ?",
            arrayOf(name.trim(), excludeId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun replaceClassifications(db: SQLiteDatabase, companyId: Long, classifications: List<Classification>) {
        db.beginTransaction()
        try {
            db.delete(TABLE_COMPANY_CLASSIFICATION, "company_id = ?", arrayOf(companyId.toString()))
            for (classification in classifications) {
                val values = ContentValues().apply {
                    put("company_id", companyId)
                    put("classification_id", classification.id)
                }
                db.insertWithOnConflict(
                    TABLE_COMPANY_CLASSIFICATION,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun getClassificationsFor(db: SQLiteDatabase, companyId: Long): List<Classification> {
        val classifications = mutableListOf<Classification>()
        db.rawQuery(
            """
            SELECT c.$COLUMN_ID FROM $TABLE_COMPANY_CLASSIFICATION cc
            JOIN $TABLE_CLASSIFICATIONS c ON c.$COLUMN_ID = cc.classification_id
            WHERE cc.company_id = ?
            ORDER BY c.$COLUMN_ID
            """.trimIndent(),
            arrayOf(companyId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                Classification.fromId(cursor.getLong(0))?.let { classifications += it }
            }
        }
        return classifications
    }

    private fun getAllClassifications(db: SQLiteDatabase): Map<Long, List<Classification>> {
        val classificationsByCompany = mutableMapOf<Long, MutableList<Classification>>()
        db.rawQuery(
            """
            SELECT cc.company_id, c.$COLUMN_ID FROM $TABLE_COMPANY_CLASSIFICATION cc
            JOIN $TABLE_CLASSIFICATIONS c ON c.$COLUMN_ID = cc.classification_id
            ORDER BY c.$COLUMN_ID
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val companyId = cursor.getLong(0)
                Classification.fromId(cursor.getLong(1))?.let { classification ->
                    classificationsByCompany.getOrPut(companyId) { mutableListOf() } += classification
                }
            }
        }
        return classificationsByCompany
    }

    private fun companyContentValues(company: Company) = ContentValues().apply {
        put(COLUMN_NAME, company.name.trim())
        put(COLUMN_URL, company.url.trim())
        put(COLUMN_PHONE, company.phone.trim())
        put(COLUMN_EMAIL, company.email.trim())
        put(COLUMN_PRODUCTS, company.products.trim())
    }

    private fun Cursor.getString(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun Cursor.getLong(column: String): Long = getLong(getColumnIndexOrThrow(column))

    companion object {
        private const val TABLE_COMPANIES = "companies"
        private const val TABLE_CLASSIFICATIONS = "classifications"
        private const val TABLE_COMPANY_CLASSIFICATION = "company_classification"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_URL = "url"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PRODUCTS = "products"
    }
}
