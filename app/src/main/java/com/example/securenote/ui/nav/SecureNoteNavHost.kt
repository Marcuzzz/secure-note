package com.example.securenote.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.securenote.ui.screens.NoteEditScreen
import com.example.securenote.ui.screens.NoteListScreen
import com.example.securenote.ui.screens.PasswordGeneratorScreen
import com.example.securenote.ui.screens.UnlockScreen
import com.example.securenote.ui.vm.UnlockViewModel

object Routes {
    const val UNLOCK = "unlock"
    const val LIST = "list"
    const val EDIT = "edit/{noteId}"
    fun edit(noteId: Long) = "edit/$noteId"
    const val GENERATOR = "generator"
}

@Composable
fun SecureNoteNavHost() {
    val nav = rememberNavController()
    val unlockVm: UnlockViewModel = viewModel(factory = UnlockViewModel.Factory)
    val unlockState by unlockVm.state.collectAsState()

    // If we lose the session, bounce to unlock.
    LaunchedEffect(unlockState.unlocked) {
        if (unlockState.unlocked) {
            nav.navigate(Routes.LIST) {
                popUpTo(Routes.UNLOCK) { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.UNLOCK) {
        composable(Routes.UNLOCK) {
            UnlockScreen(vm = unlockVm)
        }
        composable(Routes.LIST) {
            NoteListScreen(
                onOpen = { id -> nav.navigate(Routes.edit(id)) },
                onCreate = { nav.navigate(Routes.edit(0L)) },
                onGenerator = { nav.navigate(Routes.GENERATOR) },
                onLock = {
                    unlockVm.refresh()
                    nav.navigate(Routes.UNLOCK) {
                        popUpTo(Routes.LIST) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditScreen(
                noteId = id,
                onBack = { nav.popBackStack() },
                onOpenGenerator = { nav.navigate(Routes.GENERATOR) }
            )
        }
        composable(Routes.GENERATOR) {
            PasswordGeneratorScreen(onBack = { nav.popBackStack() })
        }
    }
}
