@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION", "DuplicatedCode")

package au.concepta.playwright.util

import com.microsoft.playwright.Frame
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import java.util.regex.Pattern

// ==============================================================================================
// Frame Options Extensions - see PageOptionsExtensions.kt for a description of the pattern used.
// ==============================================================================================

fun Frame.getByText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Frame.GetByTextOptions().setExact(exact))
}

fun Frame.getByText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Frame.GetByTextOptions().setExact(exact))
}

fun Frame.getByText(text: Regex, exact: Boolean? = null): Locator =
    getByText(text.toPattern(), exact)

fun Frame.getByRole(
    role: AriaRole,
    checked: Boolean? = null,
    disabled: Boolean? = null,
    expanded: Boolean? = null,
    includeHidden: Boolean? = null,
    level: Int? = null,
    name: String? = null,
    namePattern: Pattern? = null,
    nameRegex: Regex? = null,
    pressed: Boolean? = null,
    selected: Boolean? = null,
    exact: Boolean? = null,
): Locator {
    val opts = Frame.GetByRoleOptions()
    if (checked != null) opts.setChecked(checked)
    if (disabled != null) opts.setDisabled(disabled)
    if (expanded != null) opts.setExpanded(expanded)
    if (includeHidden != null) opts.setIncludeHidden(includeHidden)
    if (level != null) opts.setLevel(level)
    if (name != null) opts.setName(name)
    if (namePattern != null) opts.setName(namePattern)
    if (nameRegex != null) opts.setName(nameRegex.toPattern())
    if (pressed != null) opts.setPressed(pressed)
    if (selected != null) opts.setSelected(selected)
    if (exact != null) opts.setExact(exact)
    return getByRole(role, opts)
}

fun Frame.getByLabel(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Frame.GetByLabelOptions().setExact(exact))
}

fun Frame.getByLabel(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Frame.GetByLabelOptions().setExact(exact))
}

fun Frame.getByLabel(text: Regex, exact: Boolean? = null): Locator =
    getByLabel(text.toPattern(), exact)

fun Frame.getByPlaceholder(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Frame.GetByPlaceholderOptions().setExact(exact))
}

fun Frame.getByPlaceholder(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Frame.GetByPlaceholderOptions().setExact(exact))
}

fun Frame.getByPlaceholder(text: Regex, exact: Boolean? = null): Locator =
    getByPlaceholder(text.toPattern(), exact)

fun Frame.getByAltText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Frame.GetByAltTextOptions().setExact(exact))
}

fun Frame.getByAltText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Frame.GetByAltTextOptions().setExact(exact))
}

fun Frame.getByAltText(text: Regex, exact: Boolean? = null): Locator =
    getByAltText(text.toPattern(), exact)

fun Frame.getByTitle(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Frame.GetByTitleOptions().setExact(exact))
}

fun Frame.getByTitle(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Frame.GetByTitleOptions().setExact(exact))
}

fun Frame.getByTitle(text: Regex, exact: Boolean? = null): Locator =
    getByTitle(text.toPattern(), exact)

fun Frame.getByTestId(testId: Regex): Locator =
    getByTestId(testId.toPattern())

fun Frame.locator(
    selector: String,
    has: Locator? = null,
    hasNot: Locator? = null,
    hasText: String? = null,
    hasTextPattern: Pattern? = null,
    hasTextRegex: Regex? = null,
    hasNotText: String? = null,
    hasNotTextPattern: Pattern? = null,
    hasNotTextRegex: Regex? = null,
): Locator {
    val opts = Frame.LocatorOptions()
    if (has != null) opts.setHas(has)
    if (hasNot != null) opts.setHasNot(hasNot)
    if (hasText != null) opts.setHasText(hasText)
    if (hasTextPattern != null) opts.setHasText(hasTextPattern)
    if (hasTextRegex != null) opts.setHasText(hasTextRegex.toPattern())
    if (hasNotText != null) opts.setHasNotText(hasNotText)
    if (hasNotTextPattern != null) opts.setHasNotText(hasNotTextPattern)
    if (hasNotTextRegex != null) opts.setHasNotText(hasNotTextRegex.toPattern())
    return locator(selector, opts)
}
