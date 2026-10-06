import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const source = fs.readFileSync(
  new URL("../app/src/gecko/assets/candy_privacy/login_field_focus.js", import.meta.url),
  "utf8",
);
const context = vm.createContext({});
vm.runInContext(source, context);
const { isLoginField, install } = context.VolaLoginFieldFocus;

const input = ({ type = "text", autocomplete = "", form = null, disabled = false, readOnly = false } = {}) => ({
  tagName: "INPUT",
  type,
  disabled,
  readOnly,
  form,
  getAttribute: (name) => (name === "autocomplete" ? autocomplete : null),
});
const formWithPassword = { querySelector: () => ({}) };
const formWithoutPassword = { querySelector: () => null };

test("password fields and declared sign-in fields count", () => {
  assert.equal(isLoginField(input({ type: "password" })), true);
  assert.equal(isLoginField(input({ type: "email", autocomplete: "username" })), true);
  assert.equal(isLoginField(input({ autocomplete: "section-login email" })), true);
  assert.equal(isLoginField(input({ type: "tel", autocomplete: "one-time-code" })), true);
});

test("a plain text field counts only beside a password field", () => {
  assert.equal(isLoginField(input({ form: formWithPassword })), true);
  assert.equal(isLoginField(input({ form: formWithoutPassword })), false);
  assert.equal(isLoginField(input()), false);
});

test("search boxes, other inputs and inactive fields do not count", () => {
  assert.equal(isLoginField(input({ type: "search", form: formWithPassword })), false);
  assert.equal(isLoginField(input({ type: "checkbox", autocomplete: "username" })), false);
  assert.equal(isLoginField(input({ type: "password", disabled: true })), false);
  assert.equal(isLoginField(input({ type: "password", readOnly: true })), false);
  assert.equal(isLoginField({ tagName: "TEXTAREA", type: "", getAttribute: () => "username" }), false);
  assert.equal(isLoginField(null), false);
});

test("only the first trusted focus on a sign-in field is reported", () => {
  const listeners = [];
  const target = {
    addEventListener: (name, listener) => listeners.push({ name, listener }),
    removeEventListener: (name, listener) => {
      const index = listeners.findIndex((entry) => entry.listener === listener);
      if (index >= 0) listeners.splice(index, 1);
    },
  };
  let sent = 0;
  install(target, () => { sent += 1; });
  const focus = (event) => listeners.slice().forEach((entry) => entry.listener(event));

  focus({ isTrusted: false, target: input({ type: "password" }) });
  focus({ isTrusted: true, target: input({ type: "search" }) });
  assert.equal(sent, 0);
  focus({ isTrusted: true, target: input({ type: "password" }) });
  focus({ isTrusted: true, target: input({ type: "password" }) });
  assert.equal(sent, 1);
  assert.equal(listeners.length, 0);
});
