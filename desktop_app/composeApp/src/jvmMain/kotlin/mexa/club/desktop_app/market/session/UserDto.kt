package mexa.club.desktop_app.market.session

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String? = null,
    val email: String? = null,
)
