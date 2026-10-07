"use strict";

// Tells Vola once per document that a person started on a sign-in form, so it can offer its own
// password vault when it has none (#123, H2). Nothing about the page or the field is sent.
(() => {
  const USERNAME_TOKENS = /(?:^|\s)(?:username|email|current-password|new-password|one-time-code|webauthn)(?:\s|$)/;
  const TEXT_TYPES = new Set(["", "text", "email", "tel"]);

  function isLoginField(element) {
    if (!element || element.tagName !== "INPUT" || element.disabled || element.readOnly) return false;
    const type = String(element.type || "").toLowerCase();
    if (type === "password") return true;
    if (!TEXT_TYPES.has(type)) return false;
    const autocomplete = String(element.getAttribute?.("autocomplete") || "").toLowerCase();
    if (USERNAME_TOKENS.test(autocomplete)) return true;
    // A plain text field counts only beside a password field of the same form.
    return element.form?.querySelector?.('input[type="password"]') != null;
  }

  function install(target, send) {
    let sent = false;
    const onFocus = (event) => {
      if (sent || event.isTrusted !== true || !isLoginField(event.target)) return;
      sent = true;
      target.removeEventListener("focusin", onFocus, true);
      send();
    };
    target.addEventListener("focusin", onFocus, true);
  }

  globalThis.VolaLoginFieldFocus = { isLoginField, install };

  if (typeof document !== "undefined" && typeof browser !== "undefined" && !globalThis.__volaLoginFieldFocus) {
    globalThis.__volaLoginFieldFocus = true;
    install(document, () => {
      browser.runtime.sendMessage({ type: "login-field-focus" }).catch(() => {});
    });
  }
})();
