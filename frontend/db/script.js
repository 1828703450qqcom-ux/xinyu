const API = 'http://localhost:5000/api';
let currentTable = '';
let currentPage = 1;

// ========== 加载概览 ==========
async function loadOverview() {
    const res = await fetch(`${API}/db/stats`);
    const stats = await res.json();

    // 统计卡片
    document.getElementById('dbStats').innerHTML = `
        <div class="stat-card">
            <div class="stat-number">${stats.total_tables}</div>
            <div class="stat-label">数据表</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">${stats.total_records}</div>
            <div class="stat-label">总记录数</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">${stats.db_size_str}</div>
            <div class="stat-label">数据库大小</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">SQLite</div>
            <div class="stat-label">数据库类型</div>
        </div>
    `;

    // 表列表
    document.getElementById('tableList').innerHTML = stats.tables.map(t => `
        <div class="table-card" onclick="selectTable('${t.table}')">
            <h4>📋 ${t.table}</h4>
            <p>${t.count} 条记录</p>
        </div>
    `).join('');

    // 填充下拉框
    const select = document.getElementById('tableSelect');
    select.innerHTML = '<option value="">选择表...</option>' +
        stats.tables.map(t => `<option value="${t.table}">${t.table} (${t.count}条)</option>`).join('');
}

// ========== 选择表 ==========
function selectTable(tableName) {
    currentTable = tableName;
    currentPage = 1;
    document.getElementById('tableSelect').value = tableName;
    loadTableData();
    document.getElementById('tables').scrollIntoView({ behavior: 'smooth' });
}

// ========== 加载表数据 ==========
async function loadTableData() {
    const select = document.getElementById('tableSelect');
    currentTable = select.value;
    if (!currentTable) return;

    const res = await fetch(`${API}/db/table/${currentTable}?page=${currentPage}&per_page=20`);
    const result = await res.json();

    // 表头
    document.getElementById('tableHead').innerHTML = `
        <tr>
            ${result.columns.map(c => `<th>${c}</th>`).join('')}
            <th>操作</th>
        </tr>
    `;

    // 数据
    if (result.data.length === 0) {
        document.getElementById('tableBody').innerHTML = '';
        document.getElementById('emptyMsg').style.display = 'block';
    } else {
        document.getElementById('emptyMsg').style.display = 'none';
        document.getElementById('tableBody').innerHTML = result.data.map(row => `
            <tr>
                ${result.columns.map(c => `<td title="${row[c] || ''}">${row[c] || '-'}</td>`).join('')}
                <td>
                    <button class="btn-danger" style="padding:4px 10px;font-size:12px" onclick="deleteRow(${row.id})">删除</button>
                </td>
            </tr>
        `).join('');
    }

    // 信息
    document.getElementById('tableInfo').textContent =
        `共 ${result.total} 条记录，第 ${result.page}/${result.total_pages} 页`;

    // 分页
    renderPagination(result.total_pages);
}

// ========== 分页 ==========
function renderPagination(totalPages) {
    const pagination = document.getElementById('pagination');
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';
    for (let i = 1; i <= totalPages; i++) {
        html += `<button class="${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
    }
    pagination.innerHTML = html;
}

function goToPage(page) {
    currentPage = page;
    loadTableData();
}

// ========== 删除记录 ==========
async function deleteRow(id) {
    if (!confirm('确定要删除这条记录吗？')) return;

    await fetch(`${API}/db/table/${currentTable}/${id}`, { method: 'DELETE' });
    loadTableData();
    loadOverview();
}

// ========== 清空表 ==========
async function clearCurrentTable() {
    if (!currentTable) return;
    if (!confirm(`确定要清空 ${currentTable} 表的所有数据吗？此操作不可恢复！`)) return;

    await fetch(`${API}/db/table/${currentTable}`, { method: 'DELETE' });
    loadTableData();
    loadOverview();
}

// ========== 执行SQL查询 ==========
async function executeQuery() {
    const sql = document.getElementById('sqlInput').value.trim();
    if (!sql) return;

    try {
        const res = await fetch(`${API}/db/query`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({sql})
        });
        const result = await res.json();

        if (result.error) {
            alert('查询错误: ' + result.error);
            return;
        }

        const resultDiv = document.getElementById('queryResult');
        resultDiv.style.display = 'block';

        document.getElementById('queryInfo').textContent = `查询成功，返回 ${result.count} 条记录`;

        document.getElementById('queryHead').innerHTML = `
            <tr>${result.columns.map(c => `<th>${c}</th>`).join('')}</tr>
        `;

        document.getElementById('queryBody').innerHTML = result.data.map(row => `
            <tr>${result.columns.map(c => `<td title="${row[c] || ''}">${row[c] || '-'}</td>`).join('')}</tr>
        `).join('');
    } catch (e) {
        alert('请求失败: ' + e.message);
    }
}

function quickQuery(sql) {
    document.getElementById('sqlInput').value = sql;
    executeQuery();
}

// ========== 导航 ==========
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', function() {
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        this.classList.add('active');
    });
});

// ========== 初始化 ==========
window.onload = function() {
    loadOverview();
};
