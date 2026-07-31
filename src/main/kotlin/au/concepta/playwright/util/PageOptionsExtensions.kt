@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION", "DuplicatedCode")

package au.concepta.playwright.util

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import java.nio.file.Path
import java.util.regex.Pattern

// ====================================================================================
// Extension functions to make the options APIs on the Page class more Kotlin friendly.
//
// The Playwright Java API uses inner classes on Page as parameter objects on various
// methods. This leads to a rather convoluted API, where to set an option, one has to
// create an instance of the right options class, then call a setter on that, then
// pass the options object to the method.
//
// This file contains extension functions to allow using named and optional parameters
// instead of the options objects.
//
// In addition, regular expressions can be expressed as Kotlin's Regex, in addition
// to the Java Pattern class.
//
// This file should be limited to this purpose: make options more accessible for the
// Page class. Similar files exists for Locator and Frame.
// ====================================================================================

fun Page.getByText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Page.GetByTextOptions().setExact(exact))
}

fun Page.getByText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByText(text)
    return getByText(text, Page.GetByTextOptions().setExact(exact))
}

fun Page.getByText(text: Regex, exact: Boolean? = null): Locator =
    getByText(text.toPattern(), exact)

fun Page.getByRole(
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
    val opts = Page.GetByRoleOptions()
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

fun Page.getByLabel(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Page.GetByLabelOptions().setExact(exact))
}

fun Page.getByLabel(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByLabel(text)
    return getByLabel(text, Page.GetByLabelOptions().setExact(exact))
}

fun Page.getByLabel(text: Regex, exact: Boolean? = null): Locator =
    getByLabel(text.toPattern(), exact)

fun Page.getByPlaceholder(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Page.GetByPlaceholderOptions().setExact(exact))
}

fun Page.getByPlaceholder(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByPlaceholder(text)
    return getByPlaceholder(text, Page.GetByPlaceholderOptions().setExact(exact))
}

fun Page.getByPlaceholder(text: Regex, exact: Boolean? = null): Locator =
    getByPlaceholder(text.toPattern(), exact)

fun Page.getByAltText(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Page.GetByAltTextOptions().setExact(exact))
}

fun Page.getByAltText(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByAltText(text)
    return getByAltText(text, Page.GetByAltTextOptions().setExact(exact))
}

fun Page.getByAltText(text: Regex, exact: Boolean? = null): Locator =
    getByAltText(text.toPattern(), exact)

fun Page.getByTitle(text: String, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Page.GetByTitleOptions().setExact(exact))
}

fun Page.getByTitle(text: Pattern, exact: Boolean? = null): Locator {
    if (exact == null) return getByTitle(text)
    return getByTitle(text, Page.GetByTitleOptions().setExact(exact))
}

fun Page.getByTitle(text: Regex, exact: Boolean? = null): Locator =
    getByTitle(text.toPattern(), exact)

fun Page.getByTestId(testId: Regex): Locator =
    getByTestId(testId.toPattern())

fun Page.locator(
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
    val opts = Page.LocatorOptions()
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

fun Page.click(
    selector: String,
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
    val opts = Page.ClickOptions()
    if (button != null) opts.setButton(button)
    if (clickCount != null) opts.setClickCount(clickCount)
    if (delay != null) opts.setDelay(delay)
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    click(selector, opts)
}

fun Page.dblclick(
    selector: String,
    button: MouseButton? = null,
    delay: Double? = null,
    force: Boolean? = null,
    modifiers: List<KeyboardModifier>? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Page.DblclickOptions()
    if (button != null) opts.setButton(button)
    if (delay != null) opts.setDelay(delay)
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    dblclick(selector, opts)
}

fun Page.fill(
    selector: String,
    value: String,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    timeout: Double? = null,
) {
    val opts = Page.FillOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (timeout != null) opts.setTimeout(timeout)
    fill(selector, value, opts)
}

fun Page.check(
    selector: String,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Page.CheckOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    check(selector, opts)
}

fun Page.uncheck(
    selector: String,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Page.UncheckOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    uncheck(selector, opts)
}

fun Page.setChecked(
    selector: String,
    checked: Boolean,
    force: Boolean? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Page.SetCheckedOptions()
    if (force != null) opts.setForce(force)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    setChecked(selector, checked, opts)
}

fun Page.hover(
    selector: String,
    force: Boolean? = null,
    modifiers: List<KeyboardModifier>? = null,
    noWaitAfter: Boolean? = null,
    position: Position? = null,
    timeout: Double? = null,
    trial: Boolean? = null,
) {
    val opts = Page.HoverOptions()
    if (force != null) opts.setForce(force)
    if (modifiers != null) opts.setModifiers(modifiers)
    if (noWaitAfter != null) opts.setNoWaitAfter(noWaitAfter)
    if (position != null) opts.setPosition(position)
    if (timeout != null) opts.setTimeout(timeout)
    if (trial != null) opts.setTrial(trial)
    hover(selector, opts)
}

fun Page.navigate(
    url: String,
    referer: String? = null,
    timeout: Double? = null,
    waitUntil: WaitUntilState? = null,
): Response? {
    val opts = Page.NavigateOptions()
    if (referer != null) opts.setReferer(referer)
    if (timeout != null) opts.setTimeout(timeout)
    if (waitUntil != null) opts.setWaitUntil(waitUntil)
    return navigate(url, opts)
}

fun Page.reload(
    timeout: Double? = null,
    waitUntil: WaitUntilState? = null,
): Response? {
    val opts = Page.ReloadOptions()
    if (timeout != null) opts.setTimeout(timeout)
    if (waitUntil != null) opts.setWaitUntil(waitUntil)
    return reload(opts)
}

fun Page.waitForSelector(
    selector: String,
    state: WaitForSelectorState? = null,
    strict: Boolean? = null,
    timeout: Double? = null,
): ElementHandle? {
    val opts = Page.WaitForSelectorOptions()
    if (state != null) opts.setState(state)
    if (strict != null) opts.setStrict(strict)
    if (timeout != null) opts.setTimeout(timeout)
    return waitForSelector(selector, opts)
}

fun Page.screenshot(
    path: Path? = null,
    type: ScreenshotType? = null,
    quality: Int? = null,
    fullPage: Boolean? = null,
    clip: Clip? = null,
    omitBackground: Boolean? = null,
    timeout: Double? = null,
    animations: ScreenshotAnimations? = null,
    caret: ScreenshotCaret? = null,
    scale: ScreenshotScale? = null,
    style: String? = null,
    mask: List<Locator>? = null,
    maskColor: String? = null,
): ByteArray {
    val opts = Page.ScreenshotOptions()
    if (path != null) opts.setPath(path)
    if (type != null) opts.setType(type)
    if (quality != null) opts.setQuality(quality)
    if (fullPage != null) opts.setFullPage(fullPage)
    if (clip != null) opts.setClip(clip)
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
