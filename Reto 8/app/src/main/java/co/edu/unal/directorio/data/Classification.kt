package co.edu.unal.directorio.data

enum class Classification(val id: Long, val displayName: String) {
    CONSULTING(1L, "Consultoría"),
    CUSTOM_DEVELOPMENT(2L, "Desarrollo a la medida"),
    SOFTWARE_FACTORY(3L, "Fábrica de software");

    companion object {
        fun fromId(id: Long): Classification? = entries.firstOrNull { it.id == id }
    }
}
