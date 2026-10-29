package co.edu.unal.directorio.data

data class Company(
    val id: Long = 0L,
    val name: String,
    val url: String = "",
    val phone: String = "",
    val email: String = "",
    val products: String = "",
    val classifications: List<Classification> = emptyList()
)
