package com.barghest.bux.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileEditScreen(
    navController: NavController,
    viewModel: ProfileEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            }

            Text(
                text = "Данные профиля",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Имя пользователя") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::saveProfile,
                enabled = !state.isLoading
            ) {
                Text("Сохранить профиль")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Смена пароля",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = state.currentPassword,
                onValueChange = viewModel::updateCurrentPassword,
                label = { Text("Текущий пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::updateNewPassword,
                label = { Text("Новый пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = { Text("Повторите пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::updatePassword,
                enabled = !state.isLoading
            ) {
                Text("Обновить пароль")
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.successMessage != null) {
                Text(
                    text = state.successMessage ?: "",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
