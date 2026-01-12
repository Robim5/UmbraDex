// Localizado na raiz do projeto: C:/Users/migue/Desktop/UmbraDex_Defenitive/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// No ficheiro settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // Essencial para o Supabase e outras bibliotecas
        maven {
            url = uri("https://jitpack.io")
            // Add credentials if available
            credentials {
                username = System.getenv("JITPACK_USER") ?: ""
                password = System.getenv("JITPACK_TOKEN") ?: ""
            }
        }
    }
}


rootProject.name = "UmbraDex_Defenitive" // Ajustei o nome para corresponder à sua pasta
include(":app")
