package co.edu.unal.directorio.data

object CompanyValidator {

    enum class Field { NAME, URL, PHONE, EMAIL, PRODUCTS, CLASSIFICATIONS }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val URL_REGEX = Regex("^https?://[\\w.-]+\\.[A-Za-z]{2,}(/\\S*)?$", RegexOption.IGNORE_CASE)
    private val PHONE_REGEX = Regex("^\\+?[0-9\\s()-]{7,20}$")

    const val MIN_NAME_LENGTH = 2
    const val MIN_PRODUCTS_LENGTH = 3

    fun validate(
        name: String,
        url: String,
        phone: String,
        email: String,
        products: String,
        classificationsCount: Int
    ): Set<Field> {
        val errors = mutableSetOf<Field>()

        if (name.trim().length < MIN_NAME_LENGTH) {
            errors += Field.NAME
        }
        if (url.isNotBlank() && !URL_REGEX.matches(url.trim())) {
            errors += Field.URL
        }
        if (phone.isNotBlank() && !PHONE_REGEX.matches(phone.trim())) {
            errors += Field.PHONE
        }
        if (email.isNotBlank() && !EMAIL_REGEX.matches(email.trim())) {
            errors += Field.EMAIL
        }
        if (products.trim().length < MIN_PRODUCTS_LENGTH) {
            errors += Field.PRODUCTS
        }
        if (classificationsCount == 0) {
            errors += Field.CLASSIFICATIONS
        }

        return errors
    }

    fun normalizeName(name: String): String = name.trim().replace(Regex("\\s+"), " ")
}
