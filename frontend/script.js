const messagesContainer = document.getElementById("messages");
const textInput = document.getElementById("text-input");
const sendButton = document.getElementById("send-button");
const micButton = document.getElementById("mic-button");

const ASSISTANT_NAME = "AURA";
const STARTUP_GREETING = "Привет. Я AURA. Я рядом, можешь говорить со мной обычными словами.";

let voiceEnabled = true;
let isOnline = false;
let editingProjectItemId = null;

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
    pollReminders();
    setInterval(pollReminders, 15000);
    setupVoiceControls();
    setupAuraSettings();
    setupProjectItems();

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
                : "Я не смогла обработать запрос";
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
    }
}

async function pollReminders() {
    if (!isOnline) return;

    try {
        const response = await fetch("/api/reminders/due", { cache: "no-cache" });
        if (!response.ok) return;

        const data = await response.json();
        const reminders = Array.isArray(data.reminders) ? data.reminders : [];
        reminders.forEach((reminder) => {
            addMessage(reminder, false);
            if (window.speak && voiceEnabled) window.speak(reminder);
        });
    } catch {
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
        rateEl.value = window.voiceSettings.rate.toFixed(1);
        document.getElementById("rate-value").textContent = rateEl.value;
        rateEl.addEventListener("input", () => {
            window.voiceSettings.rate = parseFloat(rateEl.value);
            document.getElementById("rate-value").textContent = rateEl.value;
        });
    }

    if (pitchEl) {
        pitchEl.value = window.voiceSettings.pitch.toFixed(1);
        document.getElementById("pitch-value").textContent = pitchEl.value;
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
        if (window.speak) window.speak("Привет. Я AURA. Так звучит мой новый голос.");
    });
}

async function setupAuraSettings() {
    const cityEl = document.getElementById("aura-city");
    const styleEl = document.getElementById("aura-answer-style");
    const personalityEl = document.getElementById("aura-personality");
    const saveButton = document.getElementById("save-aura-settings");

    if (!cityEl || !styleEl || !personalityEl || !saveButton) return;

    try {
        const response = await fetch("/api/settings", { cache: "no-cache" });
        if (response.ok) {
            const settings = await response.json();
            cityEl.value = settings.city || "";
            styleEl.value = settings.answer_style || "коротко";
            personalityEl.value = settings.personality || "живая, дружелюбная, спокойная";
        }
    } catch {
    }

    saveButton.addEventListener("click", async () => {
        const payload = {
            city: cityEl.value.trim(),
            answer_style: styleEl.value,
            personality: personalityEl.value
        };

        try {
            const response = await fetch("/api/settings", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            addSystemMessage(response.ok ? "Настройки AURA сохранены." : "Не смогла сохранить настройки.");
        } catch {
            addSystemMessage("Backend недоступен, настройки не сохранены.");
        }
    });
}

async function setupProjectItems() {
    const form = document.getElementById("project-item-form");
    const titleEl = document.getElementById("project-item-title");
    const categoryEl = document.getElementById("project-item-category");
    const descriptionEl = document.getElementById("project-item-description");
    const submitButton = document.getElementById("project-item-submit");
    const cancelButton = document.getElementById("project-item-cancel");

    if (!form || !titleEl || !categoryEl || !descriptionEl || !submitButton || !cancelButton) return;

    await loadProjectItems();

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            title: titleEl.value.trim(),
            category: categoryEl.value.trim() || "general",
            description: descriptionEl.value.trim(),
            status: "active"
        };

        if (!payload.title) {
            addSystemMessage("CRUD: укажите название пункта.");
            return;
        }

        const url = editingProjectItemId ? `/api/project-items/${editingProjectItemId}` : "/api/project-items";
        const method = editingProjectItemId ? "PUT" : "POST";

        try {
            const response = await fetch(url, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            resetProjectItemForm();
            await loadProjectItems();
            addSystemMessage(method === "POST" ? "CRUD: пункт добавлен." : "CRUD: пункт изменен.");
        } catch {
            addSystemMessage("CRUD: не удалось сохранить пункт.");
        }
    });

    cancelButton.addEventListener("click", resetProjectItemForm);
}

async function loadProjectItems() {
    const list = document.getElementById("project-items-list");
    if (!list) return;

    try {
        const response = await fetch("/api/project-items", { cache: "no-cache" });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        renderProjectItems(Array.isArray(data.items) ? data.items : []);
    } catch {
        list.textContent = "CRUD API недоступен.";
    }
}

function renderProjectItems(items) {
    const list = document.getElementById("project-items-list");
    if (!list) return;

    list.innerHTML = "";
    if (items.length === 0) {
        const empty = document.createElement("p");
        empty.className = "project-empty";
        empty.textContent = "Пока нет пунктов. Добавьте первый.";
        list.appendChild(empty);
        return;
    }

    items.forEach((item) => {
        const row = document.createElement("article");
        row.className = `project-item ${item.status === "done" ? "done" : ""}`;

        const title = document.createElement("strong");
        title.textContent = item.title || "Untitled";

        const meta = document.createElement("span");
        meta.className = "project-meta";
        meta.textContent = `${item.category || "general"} · ${item.status || "active"}`;

        const description = document.createElement("p");
        description.textContent = item.description || "Без описания";

        const actions = document.createElement("div");
        actions.className = "project-item-actions";
        actions.append(
            projectItemButton("Изм.", () => startProjectItemEdit(item)),
            projectItemButton("Готово", () => updateProjectItemStatus(item, "done")),
            projectItemButton("Удалить", () => deleteProjectItem(item.id))
        );

        row.append(title, meta, description, actions);
        list.appendChild(row);
    });
}

function projectItemButton(label, onClick) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "project-item-button";
    button.textContent = label;
    button.addEventListener("click", onClick);
    return button;
}

function startProjectItemEdit(item) {
    editingProjectItemId = item.id;
    document.getElementById("project-item-title").value = item.title || "";
    document.getElementById("project-item-category").value = item.category || "";
    document.getElementById("project-item-description").value = item.description || "";
    document.getElementById("project-item-submit").textContent = "Изменить";
}

async function updateProjectItemStatus(item, status) {
    try {
        const response = await fetch(`/api/project-items/${item.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                title: item.title,
                category: item.category,
                description: item.description,
                status
            })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        await loadProjectItems();
        addSystemMessage("CRUD: статус заменен.");
    } catch {
        addSystemMessage("CRUD: не удалось заменить статус.");
    }
}

async function deleteProjectItem(id) {
    try {
        const response = await fetch(`/api/project-items/${id}`, { method: "DELETE" });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        await loadProjectItems();
        addSystemMessage("CRUD: пункт удален.");
    } catch {
        addSystemMessage("CRUD: не удалось удалить пункт.");
    }
}

function resetProjectItemForm() {
    editingProjectItemId = null;
    document.getElementById("project-item-form")?.reset();
    const submitButton = document.getElementById("project-item-submit");
    if (submitButton) submitButton.textContent = "Добавить";
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
