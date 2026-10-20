const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const script = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/js/bank-qr-payment.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function page(now, deadline, fetchImpl) {
  let clock = now;
  const intervals = new Map();
  const timeouts = new Map();
  const listeners = {};
  const navigations = [];
  let timerId = 0;
  const element = () => ({textContent: '', dataset: {}, classList: {add() {}}, disabled: false});
  const root = element();
  root.dataset = {
    expiresAtEpoch: String(deadline), serverNowEpoch: String(now),
    statusUrl: '/api/payment-status/REX_TEST', ticketUrl: '/bookings/1', seatMapUrl: '/booking/7'
  };
  const countdown = element();
  const statusText = element();
  const hint = element();
  const expiredPanel = element(); expiredPanel.hidden = true;
  const rebookLink = element();
  const copyButton = element(); copyButton.dataset.copyValue = 'TEST'; copyButton.addEventListener = () => {};
  const elements = {
    bankQrPayment: root, bankQrCountdown: countdown, paymentStatusText: statusText,
    bankQrHint: hint, bankQrExpired: expiredPanel, bankQrRebook: rebookLink
  };
  const window = {
    addEventListener(name, callback) { listeners[name] = callback; },
    setInterval(callback) { const id = ++timerId; intervals.set(id, callback); return id; },
    clearInterval(id) { intervals.delete(id); },
    setTimeout(callback, delay) { const id = ++timerId; timeouts.set(id, {at: clock + delay, callback}); return id; },
    location: {assign(url) { navigations.push(url); }, reload() { navigations.push('reload'); }}
  };
  const context = {
    document: {
      getElementById(id) { return elements[id]; },
      querySelector() { return element(); },
      querySelectorAll() { return [copyButton]; }
    },
    window,
    clearInterval: window.clearInterval,
    fetch: fetchImpl,
    navigator: {clipboard: {writeText: async () => {}}},
    Date: class extends Date { static now() { return clock; } },
    performance: {now() { return clock; }},
    Number, Math, String, Set
  };
  vm.runInNewContext(script, context);
  return {
    root, countdown, statusText, hint, expiredPanel, rebookLink, copyButton, navigations, listeners,
    tick(ms) {
      clock += ms;
      for (const callback of [...intervals.values()]) callback();
      for (const [id, timeout] of [...timeouts]) {
        if (timeout.at <= clock) { timeouts.delete(id); timeout.callback(); }
      }
    }
  };
}

test('three independent payment pages use their own server deadlines, even after tab pauses', () => {
  const unavailable = () => new Promise(() => {});
  const a = page(1_000_000, 1_300_000, unavailable);
  assert.equal(a.countdown.textContent, '05:00');
  a.tick(210_000);
  assert.equal(a.countdown.textContent, '01:30');
  a.listeners.pagehide();

  const b = page(1_210_000, 1_510_000, unavailable);
  assert.equal(b.countdown.textContent, '05:00');
  b.tick(270_000);
  assert.equal(b.countdown.textContent, '00:30');
  b.listeners.pagehide();

  const c = page(1_480_000, 1_780_000, unavailable);
  assert.equal(c.countdown.textContent, '05:00');
});

test('expired server status hides payment data and offers a delayed rebooking route', async () => {
  let status = 'PENDING';
  const fetchImpl = async () => ({ok: true, json: async () => ({
    status, paymentAllowed: status === 'PENDING',
    expiresAtEpochMillis: 1_003_000, serverNowEpochMillis: 1_000_000,
    rebookUrl: '/movies/9'
  })});
  const payment = page(1_000_000, 1_003_000, fetchImpl);
  await flush();
  status = 'EXPIRED';
  payment.tick(3_000);
  await flush();
  assert.equal(payment.root.dataset.paymentUnavailable, 'true');
  assert.equal(payment.copyButton.disabled, true);
  assert.equal(payment.statusText.textContent, 'Hết thời gian giữ ghế');
  assert.equal(payment.expiredPanel.hidden, false);
  assert.equal(payment.rebookLink.href, '/movies/9');
  assert.deepEqual(payment.navigations, []);
  payment.tick(4_000);
  assert.deepEqual(payment.navigations, ['/movies/9']);
});

test('network failure does not falsely mark a booking expired', async () => {
  const payment = page(1_000_000, 1_001_000, async () => { throw new Error('offline'); });
  await flush();
  payment.tick(1_000);
  await flush();
  assert.equal(payment.expiredPanel.hidden, true);
  assert.equal(payment.statusText.textContent, 'Đã đến hạn thanh toán, đang xác nhận trạng thái');
  assert.deepEqual(payment.navigations, []);
});

test('a response from a page already left cannot redirect the next checkout', async () => {
  let resolveFetch;
  const payment = page(1_000_000, 1_300_000,
    () => new Promise(resolve => { resolveFetch = resolve; }));
  payment.listeners.pagehide();
  resolveFetch({ok: true, json: async () => ({status: 'CONFIRMED'})});
  await flush();
  assert.deepEqual(payment.navigations, []);
  assert.equal(payment.root.dataset.state, undefined);
});
