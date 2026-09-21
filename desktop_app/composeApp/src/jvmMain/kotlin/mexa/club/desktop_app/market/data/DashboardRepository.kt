package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.market.api.DashboardLoadResult

interface DashboardRepository {
    suspend fun loadDashboard(page: Int = 0, size: Int = 5): DashboardLoadResult
}
