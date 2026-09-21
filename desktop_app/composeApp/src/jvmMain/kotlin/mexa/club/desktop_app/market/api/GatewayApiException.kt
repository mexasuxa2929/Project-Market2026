package mexa.club.desktop_app.market.api

class GatewayApiException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
