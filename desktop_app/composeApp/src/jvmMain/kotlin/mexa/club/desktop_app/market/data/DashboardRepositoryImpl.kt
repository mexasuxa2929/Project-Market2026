package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.market.api.loadDashboardData

class DashboardRepositoryImpl : DashboardRepository {
    override suspend fun loadDashboard(page: Int, size: Int) = loadDashboardData(page, size).also { result ->
    }
}
