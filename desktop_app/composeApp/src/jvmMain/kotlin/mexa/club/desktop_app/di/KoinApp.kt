package mexa.club.desktop_app.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

fun initKoin() {
    startKoin {
        modules(appModules)
    }
}

fun shutdownKoin() {
    stopKoin()
}
