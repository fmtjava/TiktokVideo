package com.fmt.tiktokvideo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmt.tiktokvideo.cache.UserManager
import com.fmt.tiktokvideo.cache.encryptedPrefs
import com.fmt.tiktokvideo.ui.viewmodel.LoginViewModel
import androidx.core.content.edit

/**
 * 登录页面，接口使用了：https://api.apiopen.top/swagger/index.html，感谢作者的无私奉献，可以进入进行注册，即可登录
 * 可先使用测试帐号：{
 *                  "password": "password123",
 *                  "username": "alice"
 *                }
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: LoginViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            if (uiState.loginResult != null) {
                LaunchedEffect(Unit) {
                    UserManager.save(uiState.loginResult!!.profile)
                    encryptedPrefs.edit {
                        putString("api_token", uiState.loginResult!!.token.toString())
                    }
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                return@setContent
            }
            if (!TextUtils.isEmpty(uiState.error)) {
                Toast.makeText(applicationContext, uiState.error, Toast.LENGTH_LONG).show()
            }
            var userName by remember { mutableStateOf("alice") }
            var password by remember { mutableStateOf("password123") }
            var passwordVisible by remember { mutableStateOf(false) }
            val scrollState = rememberScrollState()
            val focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 140.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "登录后，体验完整功能",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = {
                            Text("用户名")
                        },
                        placeholder = {
                            Text("请输入用户名")
                        },
                        singleLine = true,
                        modifier = Modifier.padding(top = 20.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        label = {
                            Text("密码")
                        },
                        placeholder = {
                            Text("请输入密码")
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 50.dp))
                    }

                    if (!uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .padding(top = 50.dp, start = 60.dp, end = 60.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .height(56.dp)
                                .background(color = Color.Red)
                                .clickable(true) {
                                    if (userName.isEmpty()) {
                                        Toast.makeText(
                                            applicationContext,
                                            "用户名不能为空",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@clickable
                                    }
                                    if (password.isEmpty()) {
                                        Toast.makeText(
                                            applicationContext,
                                            "密码不能为空",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@clickable
                                    }
                                    focusManager.clearFocus()
                                    viewModel.login(userName, password)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "一键登录",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }
    }
}