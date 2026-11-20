// Run with Node 20 + jsdom 24. Pass the output of PrintCommentFilterJs as argv[2].
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { JSDOM } = require('jsdom');
const source = fs.readFileSync(process.argv[2], 'utf8');
const review = '用了几天，实际续航和做工都比预期好。';
const rowClass = 'comment-main-list-item';
const replyClass = 'comment-main-list-reply-item';
const nameClass = 'comment-main-list-item-content-name';
const bodyClass = 'comment-main-list-item-content-comment';

class Xhr {
  open() {}
  setRequestHeader() {}
  addEventListener(type, handler) { this[type] = handler; }
  send() { return this.loadend(); }
}

async function test() {
  const { window } = new JSDOM(`<!doctype html><div class="comment-main">
    <div id="bot" class="${rowClass}"><span class="${nameClass}">　张大妈&nbsp;</span></div>
    <div id="parent" class="${rowClass}"><span class="${nameClass}">张大妈粉丝</span>
      <p class="${bodyClass}">张大妈</p>
      <div id="botReply" class="${replyClass}"><span class="${nameClass}">张大妈机器人</span></div>
      <div id="humanReply" class="${replyClass}"><span class="${nameClass}">值友</span>
        <span class="${nameClass}" data-comment-reply-target>张大妈</span>
        <p class="${bodyClass}"><span class="${nameClass}">张大妈</span>：${review}</p>
      </div>
    </div>
  </div><span id="unrelated" class="${nameClass}">张大妈</span>`, { runScripts: 'outside-only' });
  try {
    window.XMLHttpRequest = Xhr;
    const frames = [];
    window.requestAnimationFrame = cb => { frames.push(cb); };
    window.setInterval = () => { throw new Error('Only test mutation-driven updates'); };
    const flush = async () => {
      await new Promise(resolve => setImmediate(resolve));
      for (const frame of frames.splice(0)) frame();
    };
    const el = id => window.document.getElementById(id);
    const hidden = id => window.getComputedStyle(el(id)).display === 'none';
    window.eval(source);
    window.eval(source); // reinjection must not wrap the hooks a second time
    await flush();
    assert(hidden('bot'));
    assert(hidden('botReply'));
    for (const id of ['parent', 'humanReply', 'unrelated']) assert(!hidden(id), id);

    const late = window.document.createElement('div');
    late.id = 'late';
    late.className = replyClass;
    late.innerHTML = `<span class="${nameClass}">载入中</span>`;
    el('parent').append(late);
    await flush();
    assert(!hidden('late'));
    // In-place text mutation, not a replacement node.
    late.firstChild.firstChild.data = '张大妈';
    await flush();
    assert(hidden('late'));
    assert(!hidden('parent'));
    late.firstChild.firstChild.data = '我是张大妈';
    await flush();
    assert(!hidden('late'));
    late.firstChild.firstChild.data = '张大妈';
    const appended = late.cloneNode(true);
    appended.id = 'appended';
    el('parent').append(appended);
    await flush();
    assert(hidden('appended'));
    appended.firstChild.className = '';
    await flush();
    assert(!hidden('appended'));

    async function filtered(data, rootId) {
      const xhr = new Xhr();
      xhr.open('POST', '/ajax_comment_list');
      xhr.status = 200;
      xhr.responseText = JSON.stringify({ data });
      await xhr.send(`root_id=${rootId}&order=hot&article_id=1`);
      return JSON.parse(xhr.responseText).data;
    }
    const human = { comment_id: '1', content: '张大妈', display_name: '张大妈粉丝', receive_name: '张大妈' };
    const bot = { comment_id: '2', content: review, display_name: ' 张大妈 ' };
    const fullName = { ...bot, comment_id: '3', display_name: '张大妈机器人' };
    const typed = { ...bot, comment_id: '4', display_name: '其他展示名称', comment_type: 16 };
    const parent = { ...human, comment_id: '5', content: review, child_list: [bot, human, typed], child_total: 4 };
    const main = await filtered({ rows: [parent, bot, { ...typed, comment_type: '16' }], top: [fullName, bot], next_page: 0 }, 0);
    assert.deepEqual(main.top, []);
    assert.deepEqual(main.rows, [{ ...parent, child_list: [human], child_total: 2 }]);
    assert.deepEqual((await filtered({ rows: [bot, fullName, typed, human] }, 5)).rows, [human]);
    console.log('H5: DOM initial/dynamic rendering and XHR root/pinned/inline/lazy replies passed');
  } finally {
    window.close();
  }
}

test().catch(error => { console.error(error); process.exitCode = 1; });
