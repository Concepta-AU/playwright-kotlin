@file:Suppress("unused")

package au.concepta.playwright.util

import com.microsoft.playwright.Page

/*
This file collects helper functions that make using Playwright's objects easier in Kotlin.
 */

fun havingName(name: String) = Page.GetByRoleOptions().setName(name)!!
