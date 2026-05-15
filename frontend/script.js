// ========== 全局状态 ==========
let currentGender = localStorage.getItem('theme') || 'male';
let currentUser = null;
let currentTab = 'home';
let moodValue = 3; // 1-5 默认平静
let editingNoteId = null;
let currentTest = null;
let testAnswers = [];

// ========== 本地存储 ==========
function localGet(k) { return JSON.parse(localStorage.getItem(k) || '[]'); }
function localSet(k, v) { localStorage.setItem(k, JSON.stringify(v)); }
function getUserKey(k) {
    const u = currentUser ? currentUser.username : 'guest';
    return k + '_' + u;
}

// ========== 每日一句 ==========
const quotes = [
    { text: '你不需要很厉害才能开始，但你需要开始才能很厉害。', author: '— 佚名' },
    { text: '世界上唯一不变的，就是一切都在变。适应变化，就是成长。', author: '— 佚名' },
    { text: '不要因为走得太远，而忘记了为什么出发。', author: '— 纪伯伦' },
    { text: '你今天的努力，是幸运的伏笔。', author: '— 佚名' },
    { text: '生活不是等待暴风雨过去，而是学会在雨中跳舞。', author: '— 佚名' },
    { text: '温柔半两，从容一生。', author: '— 佚名' },
    { text: '每个人都是自己命运的建筑师。', author: '— 佚名' },
    { text: '做你自己，因为别人都有人做了。', author: '— 奥斯卡·王尔德' },
    { text: '把脸一直向着阳光，这样就不会见到阴影。', author: '— 海伦·凯勒' },
    { text: '星星发亮是为了让每一个人有一天都能找到属于自己的星星。', author: '— 小王子' },
    { text: '你值得被温柔以待，包括来自你自己的温柔。', author: '— 佚名' },
    { text: '所有的大人都曾经是小孩，虽然只有少数人记得。', author: '— 小王子' },
    { text: '生命中真正重要的不是你遭遇了什么，而是你记住了哪些事，又是如何铭记的。', author: '— 马尔克斯' },
    { text: '不管前方的路有多苦，只要走的方向正确，不管多么崎岖不平，都比站在原地更接近幸福。', author: '— 宫崎骏' },
    { text: '你的负担将变成礼物，你受的苦将照亮你的路。', author: '— 鲁米' },
    { text: '每个优秀的人，都有一段沉默的时光。', author: '— 佚名' },
    { text: '人生就像骑自行车，想保持平衡就得往前走。', author: '— 爱因斯坦' },
    { text: '世界上只有一种英雄主义，就是看清生活的真相之后依然热爱生活。', author: '— 罗曼·罗兰' },
    { text: '你不能左右天气，但可以改变心情。', author: '— 佚名' },
    { text: '愿你所有的情深意重，都能换来岁月温柔。', author: '— 佚名' },
];

// ========== 心情数据 ==========
const moodConfig = {
    1: { emoji: '😢', label: '难过', color: '#64748b' },
    2: { emoji: '😰', label: '焦虑', color: '#f59e0b' },
    3: { emoji: '😐', label: '平静', color: '#94a3b8' },
    4: { emoji: '😊', label: '开心', color: '#10b981' },
    5: { emoji: '😄', label: '超棒', color: '#6366f1' },
};

// ========== 测评数据 ==========
const tests = {
    phq9: {
        title: 'PHQ-9 抑郁筛查量表',
        questions: [
            '做事时提不起劲或没有兴趣',
            '感到心情低落、沮丧或绝望',
            '入睡困难、睡不安稳或睡眠过多',
            '感觉疲倦或没有活力',
            '食欲不振或吃太多',
            '觉得自己很糟或觉得自己很失败，让自己或家人失望',
            '对事物专注有困难，例如阅读报纸或看电视',
            '动作或说话速度变得缓慢，或坐立不安',
            '有不如死掉或用某种方式伤害自己的念头',
        ],
        options: ['完全没有', '好几天', '一半以上天数', '几乎每天'],
        levels: [
            { max: 4, level: '正常范围', desc: '你的状态看起来不错！继续保持积极的心态，记得多关心自己。' },
            { max: 9, level: '轻度', desc: '你可能正在经历一些情绪波动。尝试多和朋友聊天、运动、保持规律作息。' },
            { max: 14, level: '中度', desc: '建议你和信任的人聊聊你的感受，或寻求学校心理咨询中心的帮助。' },
            { max: 19, level: '中重度', desc: '你的情绪状态需要关注。强烈建议寻求专业心理咨询师的帮助。' },
            { max: 27, level: '重度', desc: '请尽快联系专业人士获取帮助。你不是一个人，有人愿意帮助你。' },
        ]
    },
    gad7: {
        title: 'GAD-7 焦虑筛查量表',
        questions: [
            '感到紧张、焦虑或急切',
            '不能停止或控制担忧',
            '对各种各样的事情担忧过多',
            '很难放松下来',
            '由于不安而无法静坐',
            '变得容易烦恼或急躁',
            '感到似乎将有可怕的事情发生',
        ],
        options: ['完全没有', '好几天', '一半以上天数', '几乎每天'],
        levels: [
            { max: 4, level: '正常范围', desc: '你的焦虑水平在正常范围内。继续保持良好的生活习惯。' },
            { max: 9, level: '轻度焦虑', desc: '你有一些轻度焦虑。试试深呼吸、冥想或运动来缓解。' },
            { max: 14, level: '中度焦虑', desc: '建议学习一些焦虑管理技巧，必要时寻求专业帮助。' },
            { max: 21, level: '重度焦虑', desc: '你的焦虑水平较高，建议尽快寻求专业心理咨询。' },
        ]
    },
    stress: {
        title: 'PSS 压力知觉量表',
        questions: [
            '因为意料之外的事情而感到心烦意乱',
            '感到无法控制生活中的重要事情',
            '感到紧张和有压力',
            '成功地处理恼人的生活麻烦',
            '有效地应对生活中的重要变化',
            '对自己处理个人问题的能力感到自信',
            '觉得事情按自己的意愿进行',
            '发现自己无法处理所有自己必须做的事情',
            '能够控制生活中的烦恼',
            '觉得困难堆积太多而无法克服',
        ],
        options: ['从不', '偶尔', '有时', '常常', '总是'],
        levels: [
            { max: 13, level: '低压力', desc: '你的压力水平较低。继续保持良好的应对方式！' },
            { max: 26, level: '中等压力', desc: '你承受着一定的压力。建议适当放松，找人倾诉。' },
            { max: 40, level: '高压力', desc: '你的压力水平较高。建议调整生活节奏，寻求支持。' },
        ]
    },
    sleep: {
        title: '睡眠质量评估',
        questions: [
            '你通常需要多长时间才能入睡？',
            '你一周内有几天感到睡眠不足？',
            '你是否经常在夜间醒来？',
            '你早上醒来后感觉精神饱满吗？',
            '你白天是否经常感到困倦？',
        ],
        options: ['很少', '有时', '经常', '总是'],
        levels: [
            { max: 5, level: '睡眠良好', desc: '你的睡眠质量不错！继续保持规律的作息时间。' },
            { max: 10, level: '睡眠一般', desc: '你的睡眠还有改善空间。建议减少睡前使用手机。' },
            { max: 15, level: '睡眠较差', desc: '你的睡眠质量需要改善。建议建立固定的睡前仪式。' },
            { max: 20, level: '睡眠很差', desc: '你的睡眠问题比较严重，建议咨询医生或睡眠专家。' },
        ]
    }
};

// ========== 初始化 ==========
window.addEventListener('DOMContentLoaded', async () => {
    // 检查登录
    const savedUser = localStorage.getItem('xinyu_current_user');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
        updateProfileUI();
    }

    switchTheme(currentGender);
    initGreeting();
    initWeather();
    initQuote();
    initMoodDial();
    loadMoodHistory();
    loadStats();
    loadNotes();
    loadTestHistory();
    loadTodayMood();
    loadContacts();
    loadCheckinStatus();
});

// ========== Tab 切换 ==========
function switchTab(name) {
    currentTab = name;
    document.querySelectorAll('.tab-page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
    document.getElementById('page-' + name).classList.add('active');
    document.querySelector(`.tab-item[data-tab="${name}"]`).classList.add('active');

    if (name === 'home') { loadMoodHistory(); loadTodayMood(); loadRecentNotes(); }
    if (name === 'mood') { loadMoodHistory(); loadStats(); }
    if (name === 'note') loadNotes();
    if (name === 'test') loadTestHistory();
}

// ========== 问候语 ==========
function initGreeting() {
    const hour = new Date().getHours();
    let greet, emoji;
    if (hour < 6) { greet = '夜深了'; emoji = '🌙'; }
    else if (hour < 9) { greet = '早上好'; emoji = '🌅'; }
    else if (hour < 12) { greet = '上午好'; emoji = '☀️'; }
    else if (hour < 14) { greet = '中午好'; emoji = '🌤️'; }
    else if (hour < 18) { greet = '下午好'; emoji = '☀️'; }
    else if (hour < 22) { greet = '晚上好'; emoji = '🌆'; }
    else { greet = '夜深了'; emoji = '🌙'; }

    const nick = currentUser ? currentUser.nickname : '';
    const nameStr = nick ? nick : '';
    document.getElementById('greetingText').textContent = greet + (nameStr ? '，' + nameStr : '') + ' ' + emoji;

    const subs = ['今天过得怎么样？', '记得对自己好一点', '心语一直在这里陪你', '每一步都是进步', '你值得被温柔以待'];
    document.getElementById('greetingSub').textContent = subs[Math.floor(Math.random() * subs.length)];
}

// ========== 天气 ==========
async function initWeather() {
    const card = document.getElementById('weatherCard');
    // 先显示缓存
    const cached = localStorage.getItem('weather_cache');
    if (cached) {
        const c = JSON.parse(cached);
        if (Date.now() - c.time < 3600000) {
            renderWeather(c.data);
            return;
        }
    }

    try {
        // 尝试定位
        const pos = await new Promise((resolve, reject) => {
            if (!navigator.geolocation) reject();
            navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 });
        });

        const { latitude: lat, longitude: lon } = pos.coords;
        const res = await fetch(`https://wttr.in/${lat},${lon}?format=j1&lang=zh`);
        const data = await res.json();
        localStorage.setItem('weather_cache', JSON.stringify({ data, time: Date.now() }));
        renderWeather(data);
    } catch (e) {
        // 定位失败，用默认
        try {
            const res = await fetch('https://wttr.in/?format=j1&lang=zh');
            const data = await res.json();
            localStorage.setItem('weather_cache', JSON.stringify({ data, time: Date.now() }));
            renderWeather(data);
        } catch (e2) {
            document.getElementById('weatherCity').textContent = '天气获取失败';
            document.getElementById('weatherDesc').textContent = '请检查网络连接';
        }
    }
}

function renderWeather(data) {
    try {
        const current = data.current_condition[0];
        const area = data.nearest_area[0];
        const temp = current.temp_C;
        const desc = current.lang_zh && current.lang_zh[0] ? current.lang_zh[0].value : current.weatherDesc[0].value;
        const city = area.areaName[0].value;
        const humidity = current.humidity;
        const wind = current.windspeedKmph;

        const code = parseInt(current.weatherCode);
        let icon = '🌤️';
        if (code <= 116) icon = '⛅';
        if (code <= 113) icon = '☀️';
        if (code >= 176) icon = '🌧️';
        if (code >= 263 && code <= 266) icon = '🌫️';
        if (code >= 296 && code <= 311) icon = '🌧️';
        if (code >= 314 && code <= 395) icon = '❄️';

        document.getElementById('weatherIcon').textContent = icon;
        document.getElementById('weatherTemp').textContent = temp + '°';
        document.getElementById('weatherCity').textContent = city;
        document.getElementById('weatherDesc').textContent = desc;
        document.getElementById('weatherDetail').textContent = `湿度 ${humidity}% · 风速 ${wind}km/h`;
    } catch (e) {
        document.getElementById('weatherCity').textContent = '天气解析失败';
    }
}

// ========== 每日一句 ==========
function initQuote() {
    const today = new Date().toDateString();
    const saved = localStorage.getItem('daily_quote_date');
    let idx;
    if (saved === today) {
        idx = parseInt(localStorage.getItem('daily_quote_idx') || '0');
    } else {
        idx = Math.floor(Math.random() * quotes.length);
        localStorage.setItem('daily_quote_date', today);
        localStorage.setItem('daily_quote_idx', idx);
    }
    const q = quotes[idx];
    document.getElementById('dailyQuote').textContent = q.text;
    document.getElementById('quoteAuthor').textContent = q.author;
}

// ========== 主题切换 ==========
function switchTheme(theme) {
    currentGender = theme;
    localStorage.setItem('theme', theme);
    document.body.className = 'theme-' + theme;
    const label = document.getElementById('themeLabel');
    if (label) label.textContent = theme === 'male' ? '男生版' : '女生版';
}

function toggleTheme() {
    switchTheme(currentGender === 'male' ? 'female' : 'male');
}

// ========== 心情指针盘 ==========
function initMoodDial() {
    const svg = document.querySelector('.dial-svg');
    const pointer = document.getElementById('dialPointer');
    const arc = document.getElementById('dialArc');
    const emoji = document.getElementById('dialEmoji');
    const label = document.getElementById('dialLabel');

    let isDragging = false;

    function getAngle(e) {
        const rect = svg.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
        const clientY = e.touches ? e.touches[0].clientY : e.clientY;
        let angle = Math.atan2(clientX - cx, -(clientY - cy)) * (180 / Math.PI);
        if (angle < 0) angle += 360;
        // 限制在 0-270 度范围（从 -135 到 +135）
        if (angle > 270) angle = angle > 315 ? 0 : 270;
        return Math.min(Math.max(angle, 0), 270);
    }

    function updateDial(angle) {
        const val = Math.round((angle / 270) * 4) + 1;
        const clamped = Math.min(Math.max(val, 1), 5);
        moodValue = clamped;

        pointer.setAttribute('transform', `rotate(${angle - 135} 100 100)`);

        const dashLen = (angle / 270) * 400;
        arc.setAttribute('stroke-dasharray', `${dashLen} 534`);

        const cfg = moodConfig[clamped];
        emoji.textContent = cfg.emoji;
        label.textContent = cfg.label;

        // 更新快捷按钮
        document.querySelectorAll('.mood-qbtn').forEach(b => b.classList.remove('active'));
        const activeBtn = document.querySelector(`.mood-qbtn[data-val="${clamped}"]`);
        if (activeBtn) activeBtn.classList.add('active');
    }

    function onStart(e) {
        isDragging = true;
        updateDial(getAngle(e));
        e.preventDefault();
    }

    function onMove(e) {
        if (!isDragging) return;
        updateDial(getAngle(e));
        e.preventDefault();
    }

    function onEnd() { isDragging = false; }

    svg.addEventListener('mousedown', onStart);
    svg.addEventListener('touchstart', onStart, { passive: false });
    document.addEventListener('mousemove', onMove);
    document.addEventListener('touchmove', onMove, { passive: false });
    document.addEventListener('mouseup', onEnd);
    document.addEventListener('touchend', onEnd);

    // 初始位置
    updateDial(135); // 默认中间 = 3 (平静)
}

function setMoodValue(val) {
    moodValue = val;
    const angle = ((val - 1) / 4) * 270;
    const pointer = document.getElementById('dialPointer');
    const arc = document.getElementById('dialArc');
    const emoji = document.getElementById('dialEmoji');
    const label = document.getElementById('dialLabel');

    pointer.setAttribute('transform', `rotate(${angle - 135} 100 100)`);
    const dashLen = (angle / 270) * 400;
    arc.setAttribute('stroke-dasharray', `${dashLen} 534`);

    const cfg = moodConfig[val];
    emoji.textContent = cfg.emoji;
    label.textContent = cfg.label;

    document.querySelectorAll('.mood-qbtn').forEach(b => b.classList.remove('active'));
    document.querySelector(`.mood-qbtn[data-val="${val}"]`).classList.add('active');
}

// ========== 保存心情 ==========
function submitMood() {
    const note = document.getElementById('moodNote').value.trim();
    const cfg = moodConfig[moodValue];
    const record = {
        mood: cfg.label,
        moodVal: moodValue,
        emoji: cfg.emoji,
        note,
        gender: currentGender,
        created_at: new Date().toISOString(),
    };

    const key = getUserKey('moods');
    const moods = localGet(key);
    moods.unshift(record);
    localSet(key, moods);

    document.getElementById('moodNote').value = '';
    setMoodValue(3);

    loadMoodHistory();
    loadStats();
    loadTodayMood();

    // 提示
    const btn = document.querySelector('#page-mood .btn-primary');
    const orig = btn.textContent;
    btn.textContent = '✓ 已保存';
    btn.style.background = '#10b981';
    setTimeout(() => { btn.textContent = orig; btn.style.background = ''; }, 1500);
}

// ========== 心情历史 ==========
function loadMoodHistory() {
    const moods = localGet(getUserKey('moods'));
    const el = document.getElementById('moodHistory');
    if (!moods.length) { el.innerHTML = '<div class="empty-tip">还没有心情记录</div>'; return; }
    el.innerHTML = moods.slice(0, 20).map(m => `
        <div class="mood-hist-item">
            <div class="mood-hist-emoji">${m.emoji || '😐'}</div>
            <div class="mood-hist-info">
                <div class="mood-hist-label">${m.mood}</div>
                ${m.note ? `<div class="mood-hist-note">${m.note}</div>` : ''}
            </div>
            <div class="mood-hist-time">${new Date(m.created_at).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</div>
        </div>
    `).join('');
}

// ========== 今日心情 ==========
function loadTodayMood() {
    const moods = localGet(getUserKey('moods'));
    const today = new Date().toDateString();
    const todayMoods = moods.filter(m => new Date(m.created_at).toDateString() === today);
    const card = document.getElementById('todayMoodCard');

    if (!todayMoods.length) {
        card.innerHTML = '<div class="today-mood-empty"><span>🌈</span><p>今天还没有记录心情哦</p><button onclick="switchTab(\'mood\')">去记录 →</button></div>';
        return;
    }

    const latest = todayMoods[0];
    card.innerHTML = `
        <div class="today-mood-filled">
            <div class="tm-emoji">${latest.emoji || '😐'}</div>
            <div class="tm-info">
                <div class="tm-label">${latest.mood}</div>
                ${latest.note ? `<div class="tm-note">${latest.note}</div>` : ''}
                <div class="tm-time">${new Date(latest.created_at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}</div>
            </div>
        </div>
    `;
}

// ========== 统计 ==========
function loadStats() {
    const moods = localGet(getUserKey('moods'));
    const checkins = localGet(getUserKey('checkins'));
    document.getElementById('statMoods').textContent = moods.length;
    document.getElementById('statCheckins').textContent = checkins.length;
    const happy = moods.filter(m => m.moodVal >= 4).length;
    document.getElementById('statHappy').textContent = moods.length > 0 ? Math.round(happy / moods.length * 100) + '%' : '0%';
}

// ========== 打卡 ==========
function doDailyCheckin() {
    const today = new Date().toISOString().split('T')[0];
    const key = getUserKey('checkins');
    const checkins = localGet(key);

    if (checkins.some(c => c.day === today)) {
        alert('今天已经打卡啦~');
        return;
    }

    checkins.push({ day: today, created_at: new Date().toISOString() });
    localSet(key, checkins);
    loadCheckinStatus();
    loadStats();
    alert('打卡成功！今天也加油~ ✨');
}

function loadCheckinStatus() {
    const today = new Date().toISOString().split('T')[0];
    const checkins = localGet(getUserKey('checkins'));
    const el = document.getElementById('checkinLabel');
    if (checkins.some(c => c.day === today)) {
        el.textContent = '✓ 已打卡';
        el.style.color = '#10b981';
    } else {
        el.textContent = '未打卡';
        el.style.color = '';
    }
}

// ========== 测评 ==========
function startTest(id) {
    currentTest = id;
    testAnswers = [];
    const test = tests[id];
    document.getElementById('testTitle').textContent = test.title;
    renderTestQuestion(0);
    document.getElementById('testOverlay').classList.add('active');
    document.getElementById('testModal').classList.add('active');
}

function renderTestQuestion(idx) {
    const test = tests[currentTest];
    if (idx >= test.questions.length) {
        renderTestResult();
        return;
    }

    const progress = ((idx) / test.questions.length * 100).toFixed(0);
    let html = `
        <div class="test-progress"><div class="test-progress-bar" style="width:${progress}%"></div></div>
        <div class="test-question">
            <div class="test-q-num">第 ${idx + 1} / ${test.questions.length} 题</div>
            <div class="test-q-text">${test.questions[idx]}</div>
            <div class="test-options">
    `;

    test.options.forEach((opt, i) => {
        const selected = testAnswers[idx] === i ? 'selected' : '';
        html += `<button class="test-option ${selected}" onclick="selectTestOption(${idx}, ${i})">${opt}</button>`;
    });

    html += '</div></div>';

    if (idx > 0) {
        html += `<button class="btn-secondary" onclick="renderTestQuestion(${idx - 1})" style="margin-bottom:10px">← 上一题</button>`;
    }

    document.getElementById('testBody').innerHTML = html;
}

function selectTestOption(qIdx, optIdx) {
    testAnswers[qIdx] = optIdx;
    document.querySelectorAll('.test-option').forEach(o => o.classList.remove('selected'));
    document.querySelectorAll('.test-option')[optIdx].classList.add('selected');

    setTimeout(() => {
        renderTestQuestion(qIdx + 1);
    }, 300);
}

function renderTestResult() {
    const test = tests[currentTest];
    const score = testAnswers.reduce((sum, a) => sum + (a || 0), 0);
    const level = test.levels.find(l => score <= l.max) || test.levels[test.levels.length - 1];

    // 保存结果
    const result = {
        testId: currentTest,
        testTitle: test.title,
        score,
        level: level.level,
        desc: level.desc,
        created_at: new Date().toISOString(),
    };
    const key = getUserKey('test_results');
    const results = localGet(key);
    results.unshift(result);
    localSet(key, results);

    document.getElementById('testBody').innerHTML = `
        <div class="test-result">
            <div class="test-result-score">${score}</div>
            <div class="test-result-level">${level.level}</div>
            <div class="test-result-desc">${level.desc}</div>
            <div class="test-result-actions">
                <button class="btn-secondary" onclick="closeTestModal()">关闭</button>
                <button class="btn-primary" onclick="startTest('${currentTest}')">重新测评</button>
            </div>
            <div style="margin-top:16px;padding:14px;background:#f8fafc;border-radius:12px;text-align:left;font-size:13px;color:#64748b;line-height:1.7">
                <strong style="color:#1e293b">💡 温馨提示：</strong><br>
                此量表仅供参考，不能替代专业诊断。如果你感到不适，请寻求专业帮助。
                <br><a href="tel:123205" style="color:#6366f1">心理援助热线 12320-5</a>
            </div>
        </div>
    `;
}

function closeTestModal() {
    document.getElementById('testOverlay').classList.remove('active');
    document.getElementById('testModal').classList.remove('active');
    loadTestHistory();
}

function loadTestHistory() {
    const results = localGet(getUserKey('test_results'));
    const el = document.getElementById('testHistory');
    if (!results.length) { el.innerHTML = '<div class="empty-tip">还没有测评记录</div>'; return; }
    el.innerHTML = results.slice(0, 10).map(r => `
        <div class="test-hist-item">
            <div class="test-hist-top">
                <div class="test-hist-name">${r.testTitle}</div>
                <div class="test-hist-score">${r.score}分</div>
            </div>
            <div class="test-hist-level">${r.level}</div>
            <div class="test-hist-time">${new Date(r.created_at).toLocaleString('zh-CN')}</div>
        </div>
    `).join('');

    // 更新各测评计数
    ['phq9', 'gad7', 'stress', 'sleep'].forEach(id => {
        const count = results.filter(r => r.testId === id).length;
        const el = document.getElementById(id + 'Count');
        if (el) el.textContent = count + '次测评';
    });
}

// ========== 笔记本 ==========
function createNote() {
    editingNoteId = null;
    document.getElementById('noteTitleInput').value = '';
    document.getElementById('noteContentInput').value = '';
    document.getElementById('noteOverlay').classList.add('active');
    document.getElementById('noteModal').classList.add('active');
    setTimeout(() => document.getElementById('noteTitleInput').focus(), 400);
}

function editNote(id) {
    const notes = localGet(getUserKey('notes'));
    const note = notes.find(n => n.id === id);
    if (!note) return;
    editingNoteId = id;
    document.getElementById('noteTitleInput').value = note.title;
    document.getElementById('noteContentInput').value = note.content;
    document.getElementById('noteOverlay').classList.add('active');
    document.getElementById('noteModal').classList.add('active');
}

function saveNote() {
    const title = document.getElementById('noteTitleInput').value.trim() || '无标题笔记';
    const content = document.getElementById('noteContentInput').value.trim();
    const key = getUserKey('notes');
    const notes = localGet(key);

    if (editingNoteId) {
        const idx = notes.findIndex(n => n.id === editingNoteId);
        if (idx >= 0) { notes[idx].title = title; notes[idx].content = content; notes[idx].updated_at = new Date().toISOString(); }
    } else {
        notes.unshift({ id: Date.now(), title, content, created_at: new Date().toISOString(), updated_at: new Date().toISOString() });
    }

    localSet(key, notes);
    closeNoteModal();
    loadNotes();
}

function deleteCurrentNote() {
    if (!editingNoteId || !confirm('确定删除这篇笔记？')) return;
    const key = getUserKey('notes');
    localSet(key, localGet(key).filter(n => n.id !== editingNoteId));
    closeNoteModal();
    loadNotes();
}

function closeNoteModal() {
    document.getElementById('noteOverlay').classList.remove('active');
    document.getElementById('noteModal').classList.remove('active');
    editingNoteId = null;
}

function loadNotes() {
    const notes = localGet(getUserKey('notes'));
    const el = document.getElementById('noteList');
    if (!notes.length) { el.innerHTML = '<div class="empty-tip">还没有笔记，点击右上角 + 新建 ✍️</div>'; return; }
    el.innerHTML = notes.map(n => `
        <div class="note-item" onclick="editNote(${n.id})">
            <div class="note-item-title">${n.title}</div>
            <div class="note-item-preview">${n.content || '空白笔记'}</div>
            <div class="note-item-time">${new Date(n.updated_at).toLocaleString('zh-CN')}</div>
        </div>
    `).join('');
    loadRecentNotes();
}

function loadRecentNotes() {
    const notes = localGet(getUserKey('notes'));
    const el = document.getElementById('recentNotes');
    if (!el) return;
    if (!notes.length) { el.innerHTML = '<div class="empty-tip">还没有笔记，去写一篇吧 ✍️</div>'; return; }
    el.innerHTML = notes.slice(0, 3).map(n => `
        <div class="recent-note-item" onclick="switchTab('note')">
            <div class="rn-title">${n.title}</div>
            <div class="rn-preview">${n.content || '空白笔记'}</div>
            <div class="rn-time">${new Date(n.updated_at).toLocaleString('zh-CN')}</div>
        </div>
    `).join('');
}

// ========== 求助热线 ==========
function openHelpPanel() {
    document.getElementById('helpOverlay').classList.add('active');
    document.getElementById('helpModal').classList.add('active');
}

function closeHelpPanel() {
    document.getElementById('helpOverlay').classList.remove('active');
    document.getElementById('helpModal').classList.remove('active');
}

// ========== 联系人 ==========
function openContactsPanel() {
    loadContacts();
    document.getElementById('contactOverlay').classList.add('active');
    document.getElementById('contactModal').classList.add('active');
}

function closeContactsPanel() {
    document.getElementById('contactOverlay').classList.remove('active');
    document.getElementById('contactModal').classList.remove('active');
}

function addContact() {
    const name = document.getElementById('cName').value.trim();
    const phone = document.getElementById('cPhone').value.trim();
    const relation = document.getElementById('cRelation').value;
    if (!name || !phone) { alert('请填写姓名和电话'); return; }

    const key = getUserKey('contacts');
    const contacts = localGet(key);
    contacts.push({ id: Date.now(), name, phone, relation });
    localSet(key, contacts);

    document.getElementById('cName').value = '';
    document.getElementById('cPhone').value = '';
    document.getElementById('cRelation').value = '';
    loadContacts();
}

function deleteContact(id) {
    if (!confirm('确定删除？')) return;
    const key = getUserKey('contacts');
    localSet(key, localGet(key).filter(c => c.id !== id));
    loadContacts();
}

function loadContacts() {
    const contacts = localGet(getUserKey('contacts'));
    const el = document.getElementById('contactList');
    if (!contacts.length) { el.innerHTML = '<div class="empty-tip" style="padding:16px">还没有联系人</div>'; return; }
    el.innerHTML = contacts.map(c => `
        <div class="contact-item">
            <div class="contact-item-avatar">${c.name[0]}</div>
            <div class="contact-item-info">
                <div class="contact-item-name">${c.name}</div>
                <div class="contact-item-meta">${c.relation || ''}</div>
            </div>
            <a href="tel:${c.phone.replace(/-/g, '')}" class="contact-item-call">拨打</a>
            <button class="contact-item-del" onclick="deleteContact(${c.id})">&times;</button>
        </div>
    `).join('');
}

// ========== 登录/登出 ==========
function updateProfileUI() {
    if (currentUser) {
        document.getElementById('profileName').textContent = currentUser.nickname || currentUser.username;
        document.getElementById('profileId').textContent = '@' + currentUser.username;
        document.getElementById('loginMenu').style.display = 'none';
        document.getElementById('logoutMenu').style.display = '';
    } else {
        document.getElementById('profileName').textContent = '游客';
        document.getElementById('profileId').textContent = '点击登录解锁更多功能';
        document.getElementById('loginMenu').style.display = '';
        document.getElementById('logoutMenu').style.display = 'none';
    }
}

function goLogin() { window.location.href = 'login.html'; }

function doLogout() {
    if (!confirm('确定退出登录？')) return;
    localStorage.removeItem('xinyu_current_user');
    currentUser = null;
    updateProfileUI();
    initGreeting();
}
