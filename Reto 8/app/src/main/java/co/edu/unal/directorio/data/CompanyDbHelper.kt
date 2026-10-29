package co.edu.unal.directorio.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CompanyDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE companies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                url TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                products TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE classifications (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL UNIQUE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE company_classification (
                company_id INTEGER NOT NULL,
                classification_id INTEGER NOT NULL,
                PRIMARY KEY (company_id, classification_id),
                FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
                FOREIGN KEY (classification_id) REFERENCES classifications(id)
            )
            """.trimIndent()
        )
        for (classification in Classification.entries) {
            db.execSQL(
                "INSERT INTO classifications (id, name) VALUES (?, ?)",
                arrayOf<Any>(classification.id, classification.displayName)
            )
        }
        seedCompanies(db)
    }

    private fun seedCompanies(db: SQLiteDatabase) {
        val seeds = listOf(
            SeedCompany(
                name = "Altiplano Software",
                url = "https://altiplanosoft.com",
                phone = "+57 601 743 2210",
                email = "contacto@altiplanosoft.com",
                products = "Desarrollo de aplicaciones móviles y web a la medida, " +
                    "pruebas automatizadas y soporte.",
                classificationIds = listOf(
                    Classification.CUSTOM_DEVELOPMENT.id,
                    Classification.SOFTWARE_FACTORY.id
                )
            ),
            SeedCompany(
                name = "Cauce Consultores",
                url = "https://cauceconsultores.co",
                phone = "+57 604 448 9087",
                email = "hola@cauceconsultores.co",
                products = "Consultoría en transformación digital, arquitectura de software " +
                    "y gobierno de TI.",
                classificationIds = listOf(Classification.CONSULTING.id)
            ),
            SeedCompany(
                name = "Trino Digital",
                url = "https://trinodigital.com",
                phone = "+57 604 517 3345",
                email = "info@trinodigital.com",
                products = "Fábrica de software con foco en fintech: banca digital, " +
                    "pagos y core bancario.",
                classificationIds = listOf(
                    Classification.CONSULTING.id,
                    Classification.CUSTOM_DEVELOPMENT.id,
                    Classification.SOFTWARE_FACTORY.id
                )
            )
        )

        for (seed in seeds) {
            val id = db.insert("companies", null, ContentValues().apply {
                put("name", seed.name)
                put("url", seed.url)
                put("phone", seed.phone)
                put("email", seed.email)
                put("products", seed.products)
            })
            for (classificationId in seed.classificationIds) {
                db.execSQL(
                    "INSERT INTO company_classification (company_id, classification_id) VALUES (?, ?)",
                    arrayOf<Any>(id, classificationId)
                )
            }
        }
    }

    private data class SeedCompany(
        val name: String,
        val url: String,
        val phone: String,
        val email: String,
        val products: String,
        val classificationIds: List<Long>
    )

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS company_classification")
        db.execSQL("DROP TABLE IF EXISTS companies")
        db.execSQL("DROP TABLE IF EXISTS classifications")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "companies.db"
        private const val DB_VERSION = 1
    }
}
