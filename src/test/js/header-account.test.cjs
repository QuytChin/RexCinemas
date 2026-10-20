const assert = require('node:assert/strict');
const fs = require('node:fs');

const source = fs.readFileSync('src/main/resources/static/js/header-account.js', 'utf8');
const header = fs.readFileSync('src/main/resources/templates/fragments/header.html', 'utf8');
const css = fs.readFileSync('src/main/resources/static/css/app.css', 'utf8');

assert.match(source, /await markRead\('\/api\/notifications\/'.+?\/read'\)/);
assert.match(source, /window\.location\.assign\(item\.href\)/);
assert.match(source, /if \(!response\.ok\) throw/);
assert.match(source, /X-XSRF-TOKEN/);
assert.match(source, /read-all/);
assert.match(header, /data-notification-badge/);
assert.match(header, /data-mark-all-notifications/);
assert.match(css, /\.notification-popover \.notification-list\{overflow-x:hidden;overflow-y:auto\}/);
assert.match(css, /overflow-wrap:anywhere/);
