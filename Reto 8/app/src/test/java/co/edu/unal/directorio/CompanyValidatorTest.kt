package co.edu.unal.directorio

import co.edu.unal.directorio.data.CompanyValidator
import co.edu.unal.directorio.data.CompanyValidator.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyValidatorTest {

    private fun validate(
        name: String = "Empresa XYZ",
        url: String = "",
        phone: String = "",
        email: String = "",
        products: String = "Desarrollo de software a la medida",
        classificationsCount: Int = 1
    ): Set<Field> = CompanyValidator.validate(name, url, phone, email, products, classificationsCount)

    @Test
    fun validCompanyHasNoErrors() {
        assertTrue(validate().isEmpty())
    }

    @Test
    fun blankNameIsInvalid() {
        assertTrue(Field.NAME in validate(name = "   "))
    }

    @Test
    fun nameShorterThanTwoCharactersIsInvalid() {
        assertTrue(Field.NAME in validate(name = "A"))
    }

    @Test
    fun validOptionalFieldsAreAccepted() {
        val errors = validate(
            url = "https://www.ejemplo.com",
            phone = "+57 300 123 4567",
            email = "contacto@ejemplo.com"
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidUrlIsRejected() {
        assertTrue(Field.URL in validate(url = "not-a-url"))
    }

    @Test
    fun httpUrlIsAccepted() {
        assertTrue(Field.URL !in validate(url = "http://example.co/path?a=1"))
    }

    @Test
    fun invalidPhoneIsRejected() {
        assertTrue(Field.PHONE in validate(phone = "abc"))
    }

    @Test
    fun shortPhoneIsRejected() {
        assertTrue(Field.PHONE in validate(phone = "123"))
    }

    @Test
    fun invalidEmailIsRejected() {
        assertTrue(Field.EMAIL in validate(email = "not-an-email"))
    }

    @Test
    fun blankProductsIsInvalid() {
        assertTrue(Field.PRODUCTS in validate(products = " "))
    }

    @Test
    fun missingClassificationsIsInvalid() {
        assertTrue(Field.CLASSIFICATIONS in validate(classificationsCount = 0))
    }

    @Test
    fun multipleClassificationsAreAccepted() {
        assertTrue(validate(classificationsCount = 3).isEmpty())
    }

    @Test
    fun normalizeNameTrimsAndCollapsesSpaces() {
        assertEquals("Mi Empresa", CompanyValidator.normalizeName("  Mi   Empresa  "))
    }
}
