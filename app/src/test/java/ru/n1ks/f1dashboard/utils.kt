package ru.n1ks.f1dashboard

import android.content.Intent
import org.mockito.Mockito.*

fun mockIntent(): Intent =
    mock(Intent::class.java).apply {
        val map = HashMap<String, String>()
        `when`(putExtra(anyString(), anyString())).thenAnswer { invocation ->
            map[invocation.arguments[0] as String] = invocation.arguments[1] as String
            null
        }
        `when`(getStringArrayExtra(anyString())).thenAnswer { invocation ->
            map[invocation.arguments[0] as String]
        }
    }