const API = 'http://localhost:5000/api';
let allStudents = [];

// ========== 加载学生列表 ==========
async function loadStudents(search = '') {
    const url = search ? `${API}/students?search=${encodeURIComponent(search)}` : `${API}/students`;
    const res = await fetch(url);
    allStudents = await res.json();
    renderStudents();
}

function renderStudents() {
    const list = document.getElementById('studentList');
    const emptyMsg = document.getElementById('emptyMsg');

    if (allStudents.length === 0) {
        list.innerHTML = '';
        emptyMsg.style.display = 'block';
        return;
    }

    emptyMsg.style.display = 'none';
    list.innerHTML = allStudents.map(s => `
        <tr>
            <td>${s.student_id}</td>
            <td>${s.name}</td>
            <td class="${s.gender === '男' ? 'gender-male' : 'gender-female'}">${s.gender || '-'}</td>
            <td>${s.age || '-'}</td>
            <td>${s.major || '-'}</td>
            <td>${s.class_name || '-'}</td>
            <td>${s.phone || '-'}</td>
            <td>
                <button class="action-btn btn-edit" onclick="editStudent(${s.id})">编辑</button>
                <button class="action-btn btn-delete" onclick="deleteStudent(${s.id}, '${s.name}')">删除</button>
            </td>
        </tr>
    `).join('');
}

// ========== 搜索 ==========
function searchStudents() {
    const keyword = document.getElementById('searchInput').value.trim();
    loadStudents(keyword);
}

// ========== 添加/修改学生 ==========
document.getElementById('studentForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const editId = document.getElementById('editId').value;
    const data = {
        student_id: document.getElementById('studentId').value,
        name: document.getElementById('name').value,
        gender: document.getElementById('gender').value,
        age: document.getElementById('age').value ? parseInt(document.getElementById('age').value) : null,
        major: document.getElementById('major').value,
        class_name: document.getElementById('className').value,
        phone: document.getElementById('phone').value,
        email: document.getElementById('email').value,
        address: document.getElementById('address').value
    };

    if (editId) {
        // 修改
        const res = await fetch(`${API}/students/${editId}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const result = await res.json();
        if (res.ok) {
            alert('修改成功');
            cancelEdit();
        } else {
            alert(result.error || '修改失败');
        }
    } else {
        // 添加
        const res = await fetch(`${API}/students`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const result = await res.json();
        if (res.ok) {
            alert('添加成功');
            this.reset();
        } else {
            alert(result.error || '添加失败');
        }
    }

    loadStudents();
    loadStats();
});

// ========== 编辑学生 ==========
async function editStudent(id) {
    const res = await fetch(`${API}/students/${id}`);
    const student = await res.json();

    document.getElementById('editId').value = student.id;
    document.getElementById('studentId').value = student.student_id;
    document.getElementById('name').value = student.name;
    document.getElementById('gender').value = student.gender || '';
    document.getElementById('age').value = student.age || '';
    document.getElementById('major').value = student.major || '';
    document.getElementById('className').value = student.class_name || '';
    document.getElementById('phone').value = student.phone || '';
    document.getElementById('email').value = student.email || '';
    document.getElementById('address').value = student.address || '';

    document.getElementById('formTitle').textContent = '编辑学生';
    document.getElementById('submitBtn').textContent = '保存修改';
    document.getElementById('cancelBtn').style.display = 'inline-block';

    document.getElementById('add').scrollIntoView({ behavior: 'smooth' });
}

function cancelEdit() {
    document.getElementById('editId').value = '';
    document.getElementById('studentForm').reset();
    document.getElementById('formTitle').textContent = '添加学生';
    document.getElementById('submitBtn').textContent = '添加学生';
    document.getElementById('cancelBtn').style.display = 'none';
}

// ========== 删除学生 ==========
async function deleteStudent(id, name) {
    if (!confirm(`确定要删除学生 "${name}" 吗？`)) return;

    const res = await fetch(`${API}/students/${id}`, { method: 'DELETE' });
    if (res.ok) {
        alert('删除成功');
        loadStudents();
        loadStats();
    }
}

// ========== 加载统计 ==========
async function loadStats() {
    const res = await fetch(`${API}/students/stats`);
    const stats = await res.json();

    document.getElementById('totalCount').textContent = stats.total;
    document.getElementById('maleCount').textContent = stats.male;
    document.getElementById('femaleCount').textContent = stats.female;

    const majorStats = document.getElementById('majorStats');
    if (stats.majors.length > 0) {
        majorStats.innerHTML = '<h3 style="margin-bottom:10px;font-size:14px;color:rgba(255,255,255,0.6)">专业分布</h3>' +
            stats.majors.map(m => `<span class="major-tag">${m.major} (${m.count}人)</span>`).join('');
    } else {
        majorStats.innerHTML = '';
    }
}

// ========== 导航高亮 ==========
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', function() {
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        this.classList.add('active');
    });
});

// ========== 初始化 ==========
window.onload = function() {
    loadStudents();
    loadStats();
};
