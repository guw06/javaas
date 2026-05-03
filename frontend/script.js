// ===== DOM Elements =====
const messagesContainer = document.getElementById('messages');
const textInput = document.getElementById('text-input');
const sendButton = document.getElementById('send-button');
const micButton = document.getElementById('mic-button');

let voiceEnabled = true;
let isOnline = false;

// ===== Boot Sequence =====
(function bootSequence() {
    const bar = document.getElementById('boot-progress-bar');
    const status = document.getElementById('boot-status');
    const steps = [
        { pct: 15, text: 'Загрузка ядра системы...' },
        { pct: 35, text: 'Подключение к серверу...' },
        { pct: 55, text: 'Инициализация Gemini AI...' },
        { pct: 75, text: 'Настройка голосового модуля...' },
        { pct: 90, text: 'Калибровка интерфейса...' },
        { pct: 100, text: 'Системы готовы. Добро пожаловать.' }
    ];
    let i = 0;
    const interval = setInterval(() => {
        if (i < steps.length) {
            bar.style.width = steps[i].pct + '%';
            status.textContent = steps[i].text;
            i++;
        } else {
            clearInterval(interval);
            setTimeout(() => {
                document.getElementById('boot-screen').classList.add('done');
                document.getElementById('main-ui').classList.remove('hidden');
                initApp();
            }, 600);
        }
    }, 400);
})();

// ===== Initialize App =====
function initApp() {
    initParticles();
    initSystemClock();
    checkConnection();
    setInterval(checkConnection, 5000);
    addSystemMessage('J.A.R.V.I.S. online. Все системы активны.');
    setTimeout(() => {
        addMessage('Здравствуйте! Я ваш персональный ИИ-ассистент. Чем могу помочь?', false);
    }, 500);

    // Voice settings controls
    setupVoiceControls();
}

// ===== Messages =====
function addMessage(text, isUser) {
    const div = document.createElement('div');
    div.className = `message ${isUser ? 'user-message' : 'assistant-message'}`;
    if (isUser) {
        div.textContent = text;
    } else {
        // Typing effect for assistant
        typeText(div, text);
    }
    messagesContainer.appendChild(div);
    scrollToBottom();
}

function addSystemMessage(text) {
    const div = document.createElement('div');
    div.className = 'message system-message';
    div.textContent = text;
    messagesContainer.appendChild(div);
    scrollToBottom();
}

function typeText(element, text) {
    let i = 0;
    const speed = Math.max(8, 30 - text.length / 20);
    element.textContent = '';
    function type() {
        if (i < text.length) {
            element.textContent += text.charAt(i);
            i++;
            scrollToBottom();
            setTimeout(type, speed);
        }
    }
    type();
}

function showTypingIndicator() {
    const div = document.createElement('div');
    div.className = 'typing-indicator';
    div.id = 'typing-indicator';
    div.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
    messagesContainer.appendChild(div);
    scrollToBottom();
}

function hideTypingIndicator() {
    document.getElementById('typing-indicator')?.remove();
}

function scrollToBottom() {
    const chat = document.getElementById('chat-container');
    if (chat) chat.scrollTop = chat.scrollHeight;
}

// ===== Send Command =====
async function handleSendCommand() {
    const text = textInput.value.trim();
    if (!text) return;

    sendButton.disabled = true;
    textInput.disabled = true;
    addMessage(text, true);
    textInput.value = '';
    showTypingIndicator();

    try {
        const response = await sendCommandToBackend(text);
        hideTypingIndicator();
        addMessage(response, false);
        if (window.speak && voiceEnabled) window.speak(response);
    } catch (error) {
        hideTypingIndicator();
        const errDiv = document.createElement('div');
        errDiv.className = 'message error-message';
        errDiv.textContent = !navigator.onLine ? '❌ Нет интернета' :
            !isOnline ? '❌ Сервер недоступен' : '❌ Ошибка обработки';
        messagesContainer.appendChild(errDiv);
        scrollToBottom();
    } finally {
        sendButton.disabled = false;
        textInput.disabled = false;
        textInput.focus();
    }
}
window.handleSendCommand = handleSendCommand;

// Events
sendButton.addEventListener('click', handleSendCommand);
textInput.addEventListener('keypress', e => { if (e.key === 'Enter') handleSendCommand(); });

// Mic button
micButton.addEventListener('click', () => {
    if (window.startVoiceRecognition) window.startVoiceRecognition();
});
micButton.addEventListener('dblclick', () => {
    if (window.toggleContinuousMode) {
        const on = window.toggleContinuousMode();
        addSystemMessage(on ? '🎯 Режим постоянной прослушки включён (wake words: джарвис, алиса, ассистент)' : '🛑 Режим прослушки выключен');
    }
});

// Hint chips & command items
document.querySelectorAll('.hint-chip, .command-item').forEach(el => {
    el.addEventListener('click', () => {
        const cmd = el.dataset.cmd;
        if (cmd) { textInput.value = cmd; handleSendCommand(); }
    });
});

// ===== Connection =====
async function checkConnection() {
    try {
        const r = await fetch('/ping', { cache: 'no-cache' });
        if (r.ok && !isOnline) {
            isOnline = true;
            updateConnectionUI(true);
        }
    } catch {
        if (isOnline) {
            isOnline = false;
            updateConnectionUI(false);
        }
    }
    // Update system memory
    try {
        const r = await fetch('/api/status');
        if (r.ok) {
            const d = await r.json();
            const mem = document.getElementById('system-mem');
            if (mem && d.memory) mem.textContent = `MEM: ${d.memory.percentage}%`;
        }
    } catch {}
}

function updateConnectionUI(online) {
    const badge = document.getElementById('connection-status');
    const text = document.getElementById('status-text');
    if (online) {
        badge.className = 'status-badge online';
        text.textContent = 'ONLINE';
    } else {
        badge.className = 'status-badge offline';
        text.textContent = 'OFFLINE';
    }
}

// ===== System Clock =====
function initSystemClock() {
    function update() {
        const el = document.getElementById('system-time');
        if (el) el.textContent = new Date().toLocaleTimeString('ru-RU');
    }
    update();
    setInterval(update, 1000);
}

// ===== Voice Controls =====
function setupVoiceControls() {
    const rateEl = document.getElementById('voice-rate');
    const pitchEl = document.getElementById('voice-pitch');
    const volumeEl = document.getElementById('voice-volume');

    if (rateEl) rateEl.addEventListener('input', () => {
        window.voiceSettings.rate = parseFloat(rateEl.value);
        document.getElementById('rate-value').textContent = rateEl.value;
    });
    if (pitchEl) pitchEl.addEventListener('input', () => {
        window.voiceSettings.pitch = parseFloat(pitchEl.value);
        document.getElementById('pitch-value').textContent = pitchEl.value;
    });
    if (volumeEl) volumeEl.addEventListener('input', () => {
        window.voiceSettings.volume = parseFloat(volumeEl.value);
        document.getElementById('volume-value').textContent = volumeEl.value;
    });

    document.getElementById('test-voice')?.addEventListener('click', () => {
        if (window.speak) window.speak('Тестирование голоса. Я ваш персональный ассистент Джарвис.');
    });
}

// ===== Top-bar buttons =====
document.getElementById('voice-toggle')?.addEventListener('click', function() {
    voiceEnabled = !voiceEnabled;
    this.classList.toggle('active', voiceEnabled);
    this.textContent = voiceEnabled ? '🔊' : '🔇';
    addSystemMessage(voiceEnabled ? '🔊 Озвучивание включено' : '🔇 Озвучивание выключено');
});

document.getElementById('settings-toggle')?.addEventListener('click', () => {
    document.getElementById('settings-panel')?.classList.toggle('open');
    document.getElementById('commands-panel')?.classList.remove('open');
});

document.getElementById('history-toggle')?.addEventListener('click', () => {
    document.getElementById('commands-panel')?.classList.toggle('open');
    document.getElementById('settings-panel')?.classList.remove('open');
});

document.querySelectorAll('.panel-close').forEach(btn => {
    btn.addEventListener('click', () => {
        const panel = btn.dataset.panel;
        if (panel) document.getElementById(panel)?.classList.remove('open');
    });
});

// ===== Particles =====
function initParticles() {
    const canvas = document.getElementById('particles-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let w, h, particles = [];

    function resize() {
        w = canvas.width = window.innerWidth;
        h = canvas.height = window.innerHeight;
    }
    resize();
    window.addEventListener('resize', resize);

    for (let i = 0; i < 60; i++) {
        particles.push({
            x: Math.random() * w, y: Math.random() * h,
            vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.3,
            size: Math.random() * 2 + 0.5, alpha: Math.random() * 0.3 + 0.1
        });
    }

    function draw() {
        requestAnimationFrame(draw);
        ctx.clearRect(0, 0, w, h);
        particles.forEach(p => {
            p.x += p.vx; p.y += p.vy;
            if (p.x < 0) p.x = w; if (p.x > w) p.x = 0;
            if (p.y < 0) p.y = h; if (p.y > h) p.y = 0;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(0, 212, 255, ${p.alpha})`;
            ctx.fill();
        });
        // Lines between nearby particles
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 120) {
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = `rgba(0, 212, 255, ${0.06 * (1 - dist / 120)})`;
                    ctx.stroke();
                }
            }
        }
    }
    draw();
}

// Network events
window.addEventListener('online', () => { updateConnectionUI(true); addSystemMessage('🌐 Интернет восстановлен'); });
window.addEventListener('offline', () => { updateConnectionUI(false); addSystemMessage('🌐 Нет интернета'); });
