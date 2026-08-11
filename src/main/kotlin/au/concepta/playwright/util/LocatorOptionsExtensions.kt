@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION", "DuplicatedCode")

package au.concepta.playwright.util

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.KeyboardModifier
import com.microsoft.playwright.options.MouseButton
import com.microsoft.playwright.options.Position
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotScale
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.WaitForSelectorState
import java.nio.file.Path
import java.util.regex.Pattern

// ================================================================================================
// Locator Options Extensions - see PageOptionsExtensions.kt for a description of the pattern used.
// ================================================================================================

fun Locator.getByText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Locator.GetByTextOptions().setExact(exact))
}

fun Locator.getByText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Locator.GetByTextOptions().setExact(exact))
}

fun Locator.getByText(text: Regex, exact: Boolean? = null): Locator =
    getByText(text.toPattern(), exact)

fun Locator.getByRole(
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
    val opts = Locator.GetByRoleOptions()
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

fun Locator.getByLabel(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Locator.GetByLabelOptions().setExact(exact))
}

fun Locator.getByLabel(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Locator.GetByLabelOptions().setExact(exact))
}

fun Locator.getByLabel(text: Regex, exact: Boolean? = null): Locator =
    getByLabel(text.toPattern(), exact)

fun Locator.getByPlaceholder(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Locator.GetByPlaceholderOptions().setExact(exact))
}

fun Locator.getByPlaceholder(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Locator.GetByPlaceholderOptions().setExact(exact))
}

fun Locator.getByPlaceholder(text: Regex, exact: Boolean? = null): Locator =
    getByPlaceholder(text.toPattern(), exact)

fun Locator.getByAltText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Locator.GetByAltTextOptions().setExact(exact))
}

fun Locator.getByAltText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Locator.GetByAltTextOptions().setExact(exact))
}

fun Locator.getByAltText(text: Regex, exact: Boolean? = null): Locator =
    getByAltText(text.toPattern(), exact)

fun Locator.getByTitle(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Locator.GetByTitleOptions().setExact(exact))
}

fun Locator.getByTitle(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Locator.GetByTitleOptions().setExact(exact))
}

fun Locator.getByTitle(text: Regex, exact: Boolean? = null): Locator =
    getByTitle(text.toPattern(), exact)

fun Locator.getByTestId(testId: Regex): Locator =
    getByTestId(testId.toPattern())

fun Locator.locator(
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
    val opts = Locator.LocatorOptions()
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

fun Locator.click(
    button: MouseButton? = null,
    clickCount: Int? = null,
    delay: Double? = null,
    force: Boolean? = null,
    modifiers: List<KeyboardModifier>? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.ClickOptions()
    if (button != null) opts.setButton(button)
    if (clickCount != null) opts.setClickCount(clickCount)
    if (delay != null) opts.setDelay(delay)
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    click(opts)
}

fun Locator.dblclick(
    button: MouseButton? = null,
    delay: Double? = null,
    force: Boolean? = null,
    modifiers: List<KeyboardModifier>? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.DblclickOptions()
    if (button != null) opts.setButton(button)
    if (delay != null) opts.setDelay(delay)
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    dblclick(opts)
}

fun Locator.fill(
    value: String,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    timeout: Double? = null,
) {
    val opts = Locator.FillOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (timeout != null) opts.setTimeout(timeout)
    fill(value, opts)
}

fun Locator.check(
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.CheckOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    check(opts)
}

fun Locator.uncheck(
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.UncheckOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    uncheck(opts)
}

fun Locator.setChecked(
    checked: Boolean,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.SetCheckedOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    setChecked(checked, opts)
}

fun Locator.hover(
    force: Boolean? = null,
    modifiers: List<KeyboardModifier>? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.HoverOptions()
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    hover(opts)
}

fun Locator.press(
    key: String,
    delay: Double? = null,
    noWaitAfter: Boolean? = null,
    timeout: Double? = null,
) {
    val opts = Locator.PressOptions()
    if (delay != null) opts.setDelay(delay)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (timeout != null) opts.setTimeout(timeout)
    press(key, opts)
}

fun Locator.type(
    text: String,
    delay: Double? = null,
    noWaitAfter: Boolean? = null,
    timeout: Double? = null,
) {
    val opts = Locator.TypeOptions()
    if (delay != null) opts.setDelay(delay)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (timeout != null) opts.setTimeout(timeout)
    type(text, opts)
}

fun Locator.pressSequentially(
    text: String,
    delay: Double? = null,
    noWaitAfter: Boolean? = null,
    timeout: Double? = null,
) {
    val opts = Locator.PressSequentiallyOptions()
    if (delay != null) opts.setDelay(delay)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (timeout != null) opts.setTimeout(timeout)
    pressSequentially(text, opts)
}

fun Locator.waitFor(
    state: WaitForSelectorState? = null,
    timeout: Double? = null,
) {
    val opts = Locator.WaitForOptions()
    if (state != null) opts.setState(state)
    if (timeout != null) opts.setTimeout(timeout)
    waitFor(opts)
}

fun Locator.screenshot(
    path: Path? = null,
    type: ScreenshotType? = null,
    quality: Int? = null,
    omitBackground: Boolean? = null,
    timeout: Double? = null,
    animations: ScreenshotAnimations? = null,
    caret: ScreenshotCaret? = null,
    scale: ScreenshotScale? = null,
    style: String? = null,
    mask: List<Locator>? = null,
    maskColor: String? = null,
): ByteArray {
    val opts = Locator.ScreenshotOptions()
    if (path != null) opts.setPath(path)
    if (type != null) opts.setType(type)
    if (quality != null) opts.setQuality(quality)
    if (omitBackground != null) opts.setOmitBackground(omitBackground)
    if (timeout != null) opts.setTimeout(timeout)
    if (animations != null) opts.setAnimations(animations)
    if (caret != null) opts.setCaret(caret)
    if (scale != null) opts.setScale(scale)
    if (style != null) opts.setStyle(style)
    if (mask != null) opts.setMask(mask)
    if (maskColor != null) opts.setMaskColor(maskColor)
    return screenshot(opts)
}

fun Locator.filter(
    has: Locator? = null,
    hasNot: Locator? = null,
    hasText: String? = null,
    hasTextPattern: Pattern? = null,
    hasTextRegex: Regex? = null,
    hasNotText: String? = null,
    hasNotTextPattern: Pattern? = null,
    hasNotTextRegex: Regex? = null,
): Locator {
    val opts = Locator.FilterOptions()
    if (has != null) opts.setHas(has)
    if (hasNot != null) opts.setHasNot(hasNot)
    if (hasText != null) opts.setHasText(hasText)
    if (hasTextPattern != null) opts.setHasText(hasTextPattern)
    if (hasTextRegex != null) opts.setHasText(hasTextRegex.toPattern())
    if (hasNotText != null) opts.setHasNotText(hasNotText)
    if (hasNotTextPattern != null) opts.setHasNotText(hasNotTextPattern)
    if (hasNotTextRegex != null) opts.setHasNotText(hasNotTextRegex.toPattern())
    return filter(opts)
}

fun Locator.dragTo(
    target: Locator,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    sourcePosition: Position? = null,
    targetPosition: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Locator.DragToOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (sourcePosition != null) opts.setSourcePosition(sourcePosition)
    if (targetPosition != null) opts.setTargetPosition(targetPosition)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    dragTo(target, opts)
}
