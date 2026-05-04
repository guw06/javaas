const messagesContainer = document.getElementById("messages");
const textInput = document.getElementById("text-input");
const sendButton = document.getElementById("send-button");
const micButton = document.getElementById("mic-button");

const ASSISTANT_NAME = "AURA";
const STARTUP_GREETING = "Здравствуйте. Я AURA, ваш персональный ассистент. Система готова, я на связи.";

let voiceEnabled = true;
let isOnline = false;

(function bootSequence() {
    const bar = document.getElementById("boot-progress-bar");
    const status = document.getElementById("boot-status");
    const steps = [
        { pct: 14, text: "Проверяю локальное ядро..." },
        { pct: 32, text: "Подключаю голосовой модуль..." },
        { pct: 49, text: "Синхронизирую память..." },
        { pct: 68, text: "Готовлю командный канал..." },
        { pct: 86, text: "Настраиваю рабочую станцию..." },
        { pct: 100, text: `${ASSISTANT_NAME} готова.` }
    ];

    let i = 0;
    const interval = setInterval(() => {
        if (i < steps.length) {
            bar.style.width = `${steps[i].pct}%`;
            status.textContent = steps[i].text;
            i++;
            return;
        }

        clearInterval(interval);
        setTimeout(() => {
            document.getElementById("boot-screen").classList.add("done");
            document.getElementById("main-ui").classList.remove("hidden");
            initApp();
        }, 500);
    }, 360);
})();

function initApp() {
    initParticles();
    initSystemClock();
    checkConnection();
    setInterval(checkConnection, 5000);
    setupVoiceControls();

    addSystemMessage(`${ASSISTANT_NAME} online. Канал открыт.`);
    setTimeout(() => {
        addMessage(STARTUP_GREETING, false);
        if (window.speak && voiceEnabled) {
            setTimeout(() => window.speak(STARTUP_GREETING), 450);
        }
    }, 420);
}

function addMessage(text, isUser) {
    const div = document.createElement("div");
    div.className = `message ${isUser ? "user-message" : "assistant-message"}`;

    if (isUser) {
        div.textContent = text;
    } else {
        typeText(div, text);
    }

    messagesContainer.appendChild(div);
    scrollToBottom();
}

function addSystemMessage(text) {
    const div = document.createElement("div");
    div.className = "message system-message";
    div.textContent = text;
    messagesContainer.appendChild(div);
    scrollToBottom();
}

function typeText(element, text) {
    let i = 0;
    const speed = Math.max(7, 24 - text.length / 26);
    element.textContent = "";

    function type() {
        if (i >= text.length) return;
        element.textContent += text.charAt(i);
        i++;
        scrollToBottom();
        setTimeout(type, speed);
    }

    type();
}

function showTypingIndicator() {
    const div = document.createElement("div");
    div.className = "typing-indicator";
    div.id = "typing-indicator";
    div.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
    messagesContainer.appendChild(div);
    scrollToBottom();
}

function hideTypingIndicator() {
    document.getElementById("typing-indicator")?.remove();
}

function scrollToBottom() {
    const chat = document.getElementById("chat-container");
    if (chat) chat.scrollTop = chat.scrollHeight;
}

async function handleSendCommand() {
    const text = textInput.value.trim();
    if (!text) return;

    sendButton.disabled = true;
    textInput.disabled = true;
    addMessage(text, true);
    textInput.value = "";
    showTypingIndicator();

    try {
        const response = await sendCommandToBackend(text);
        hideTypingIndicator();
        addMessage(response, false);
        if (window.speak && voiceEnabled) window.speak(response);
    } catch {
        hideTypingIndicator();
        const errDiv = document.createElement("div");
        errDiv.className = "message error-message";
        errDiv.textContent = !navigator.onLine
            ? "Нет интернет-соединения"
            : !isOnline
                ? "Backend недоступен"
                : "Не удалось обработать запрос";
        messagesContainer.appendChild(errDiv);
        scrollToBottom();
    } finally {
        sendButton.disabled = false;
        textInput.disabled = false;
        textInput.focus();
    }
}

window.handleSendCommand = handleSendCommand;

sendButton.addEventListener("click", handleSendCommand);
textInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") handleSendCommand();
});

micButton.addEventListener("click", () => {
    if (window.startVoiceRecognition) window.startVoiceRecognition();
});

micButton.addEventListener("dblclick", () => {
    if (!window.toggleContinuousMode) return;

    const on = window.toggleContinuousMode();
    addSystemMessage(on
        ? "Постоянное прослушивание включено."
        : "Постоянное прослушивание выключено.");
});

document.querySelectorAll(".hint-chip, .command-item").forEach((el) => {
    el.addEventListener("click", () => {
        const cmd = el.dataset.cmd;
        if (!cmd) return;
        textInput.value = cmd;
        handleSendCommand();
    });
});

async function checkConnection() {
    try {
        const response = await fetch("/ping", { cache: "no-cache" });
        if (response.ok) {
            isOnline = true;
            updateConnectionUI(true);
        }
    } catch {
        isOnline = false;
        updateConnectionUI(false);
    }

    try {
        const response = await fetch("/api/status", { cache: "no-cache" });
        if (!response.ok) return;

        const data = await response.json();
        const mem = document.getElementById("system-mem");
        if (mem && data.memory) mem.textContent = `MEM: ${data.memory.percentage}%`;
    } catch {
        // Status is optional for the visual shell.
    }
}

function updateConnectionUI(online) {
    const badge = document.getElementById("connection-status");
    const text = document.getElementById("status-text");
    const runtime = document.getElementById("runtime-backend");

    if (online) {
        badge.className = "status-badge online";
        text.textContent = "ONLINE";
        if (runtime) runtime.textContent = "active";
        return;
    }

    badge.className = "status-badge offline";
    text.textContent = "OFFLINE";
    if (runtime) runtime.textContent = "offline";
}

function initSystemClock() {
    function update() {
        const el = document.getElementById("system-time");
        if (el) el.textContent = new Date().toLocaleTimeString("ru-RU");
    }

    update();
    setInterval(update, 1000);
}

function setupVoiceControls() {
    const rateEl = document.getElementById("voice-rate");
    const pitchEl = document.getElementById("voice-pitch");
    const volumeEl = document.getElementById("voice-volume");

    if (rateEl) {
        rateEl.addEventListener("input", () => {
            window.voiceSettings.rate = parseFloat(rateEl.value);
            document.getElementById("rate-value").textContent = rateEl.value;
        });
    }

    if (pitchEl) {
        pitchEl.addEventListener("input", () => {
            window.voiceSettings.pitch = parseFloat(pitchEl.value);
            document.getElementById("pitch-value").textContent = pitchEl.value;
        });
    }

    if (volumeEl) {
        volumeEl.addEventListener("input", () => {
            window.voiceSettings.volume = parseFloat(volumeEl.value);
            document.getElementById("volume-value").textContent = volumeEl.value;
        });
    }

    document.getElementById("test-voice")?.addEventListener("click", () => {
        if (window.speak) window.speak("Проверка голоса. AURA готова к работе.");
    });
}

document.getElementById("voice-toggle")?.addEventListener("click", function () {
    voiceEnabled = !voiceEnabled;
    this.classList.toggle("active", voiceEnabled);
    addSystemMessage(voiceEnabled ? "Озвучивание включено." : "Озвучивание выключено.");
});

document.getElementById("settings-toggle")?.addEventListener("click", () => {
    document.getElementById("settings-panel")?.classList.toggle("open");
    document.getElementById("commands-panel")?.classList.remove("open");
});

document.getElementById("history-toggle")?.addEventListener("click", () => {
    document.getElementById("commands-panel")?.classList.toggle("open");
    document.getElementById("settings-panel")?.classList.remove("open");
});

document.querySelectorAll(".panel-close").forEach((btn) => {
    btn.addEventListener("click", () => {
        const panel = btn.dataset.panel;
        if (panel) document.getElementById(panel)?.classList.remove("open");
    });
});

function initParticles() {
    const canvas = document.getElementById("particles-canvas");
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    const colors = [
        "rgba(143, 227, 193, ",
        "rgba(233, 184, 114, ",
        "rgba(177, 145, 255, "
    ];
    let w;
    let h;
    let particles = [];

    function resize() {
        w = canvas.width = window.innerWidth * window.devicePixelRatio;
        h = canvas.height = window.innerHeight * window.devicePixelRatio;
        canvas.style.width = `${window.innerWidth}px`;
        canvas.style.height = `${window.innerHeight}px`;
    }

    resize();
    window.addEventListener("resize", resize);

    for (let i = 0; i < 52; i++) {
        particles.push({
            x: Math.random() * w,
            y: Math.random() * h,
            vx: (Math.random() - 0.5) * 0.18 * window.devicePixelRatio,
            vy: (Math.random() - 0.5) * 0.18 * window.devicePixelRatio,
            size: (Math.random() * 2.4 + 0.8) * window.devicePixelRatio,
            alpha: Math.random() * 0.18 + 0.08,
            color: colors[Math.floor(Math.random() * colors.length)]
        });
    }

    function draw() {
        requestAnimationFrame(draw);
        ctx.clearRect(0, 0, w, h);

        particles.forEach((p) => {
            p.x += p.vx;
            p.y += p.vy;
            if (p.x < 0) p.x = w;
            if (p.x > w) p.x = 0;
            if (p.y < 0) p.y = h;
            if (p.y > h) p.y = 0;

            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fillStyle = `${p.color}${p.alpha})`;
            ctx.fill();
        });

        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                const max = 125 * window.devicePixelRatio;

                if (dist < max) {
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = `rgba(230, 221, 197, ${0.045 * (1 - dist / max)})`;
                    ctx.stroke();
                }
            }
        }
    }

    draw();
}

window.addEventListener("online", () => {
    updateConnectionUI(true);
    addSystemMessage("Интернет-соединение восстановлено.");
});

window.addEventListener("offline", () => {
    updateConnectionUI(false);
    addSystemMessage("Интернет-соединение потеряно.");
});
