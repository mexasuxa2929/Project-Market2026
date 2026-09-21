package mexa.club.desktop_app.di

import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.localization.AuthNetworkStrings
import mexa.club.desktop_app.market.data.DashboardRepository
import mexa.club.desktop_app.market.data.DashboardRepositoryImpl
import mexa.club.desktop_app.market.data.RolesRepository
import mexa.club.desktop_app.market.data.RolesRepositoryImpl
import mexa.club.desktop_app.market.data.UsersRepository
import mexa.club.desktop_app.market.data.UsersRepositoryImpl
import mexa.club.desktop_app.market.presentation.DashboardScreenModel
import mexa.club.desktop_app.market.roles.RolesScreenModel
import mexa.club.desktop_app.market.users.UsersScreenModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val dataModule = module {
    factory { (net: AuthNetworkStrings) -> AuthApiClient(net = net) }
    factory<UsersRepository> { (net: AuthNetworkStrings) ->
        UsersRepositoryImpl(get { parametersOf(net) })
    }
    factory<RolesRepository> { (net: AuthNetworkStrings) ->
        RolesRepositoryImpl(get { parametersOf(net) })
    }
    single<DashboardRepository> { DashboardRepositoryImpl() }
}

val screenModelModule = module {
    factory { (net: AuthNetworkStrings) ->
        UsersScreenModel(get { parametersOf(net) })
    }
    factory { (net: AuthNetworkStrings) ->
        RolesScreenModel(get { parametersOf(net) })
    }
    single { DashboardScreenModel(get()) }
}

val appModules = listOf(dataModule, screenModelModule)
