const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
let continuousMode = false;
const wakeWords = ["аура", "джарвис", "ассистент", "алиса"];

let voiceSettings = {
    voice: null,
    rate: 1.0,
    pitch: 1.0,
    volume: 1.0
};

let audioContext = null;
let analyser = null;
let visualizerAnimId = null;

if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.lang = "ru-RU";
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
} else {
    console.warn("Web Speech API is not supported in this browser.");
    window.addEventListener("DOMContentLoaded", () => {
        const voiceState = document.getElementById("runtime-voice");
        if (voiceState) voiceState.textContent = "unavailable";
    });
}

async function sendCommandToBackend(text) {
    try {
        const response = await fetch("/api/command", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text })
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        return data.response;
    } catch (error) {
        console.error("Backend error:", error);
        return "Не удалось связаться с сервером.";
    }
}

function speak(text) {
    if (!window.speechSynthesis) return;

    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "ru-RU";
    utterance.rate = voiceSettings.rate;
    utterance.pitch = voiceSettings.pitch;
    utterance.volume = voiceSettings.volume;
    if (voiceSettings.voice) utterance.voice = voiceSettings.voice;

    utterance.onstart = () => {
        document.getElementById("mic-button")?.classList.add("speaking");
    };

    utterance.onend = () => {
        document.getElementById("mic-button")?.classList.remove("speaking");
    };

    window.speechSynthesis.speak(utterance);
}

window.speak = speak;

function loadVoices() {
    const voices = window.speechSynthesis?.getVoices() || [];
    const select = document.getElementById("voice-select");
    if (!select) return;

    const ruVoices = voices.filter((voice) => voice.lang.toLowerCase().startsWith("ru"));
    const availableVoices = ruVoices.length > 0 ? ruVoices : voices;

    select.innerHTML = "";
    availableVoices.forEach((voice, index) => {
        const option = document.createElement("option");
        option.value = String(index);
        option.textContent = `${voice.name} (${voice.lang})`;
        if (voice.default) option.selected = true;
        select.appendChild(option);
    });

    if (availableVoices.length > 0 && !voiceSettings.voice) {
        voiceSettings.voice = availableVoices[select.selectedIndex >= 0 ? select.selectedIndex : 0];
    }

    select.onchange = () => {
        voiceSettings.voice = availableVoices[parseInt(select.value, 10)] || null;
    };
}

if (window.speechSynthesis) {
    speechSynthesis.onvoiceschanged = loadVoices;
    window.addEventListener("DOMContentLoaded", loadVoices);
}

function startVoiceRecognition() {
    if (!recognition) {
        alert("Распознавание речи не поддерживается в этом браузере.");
        return;
    }

    try {
        recognition.start();
    } catch (error) {
        console.error(error);
    }
}

function toggleContinuousMode() {
    if (!recognition) return false;

    continuousMode = !continuousMode;
    recognition.continuous = continuousMode;
    const mic = document.getElementById("mic-button");
    const badge = document.getElementById("mode-badge");
    const modeText = document.getElementById("mode-text");

    if (continuousMode) {
        try {
            recognition.start();
        } catch {
            // Recognition can already be running.
        }
        mic?.classList.add("continuous");
        badge?.classList.add("active");
        if (modeText) modeText.textContent = "listening";
    } else {
        try {
            recognition.stop();
        } catch {
            // Recognition can already be stopped.
        }
        mic?.classList.remove("continuous", "listening");
        badge?.classList.remove("active");
        if (modeText) modeText.textContent = "wake mode";
    }

    return continuousMode;
}

if (recognition) {
    recognition.onstart = () => {
        document.getElementById("mic-button")?.classList.add("listening");
        const voiceState = document.getElementById("runtime-voice");
        if (voiceState) voiceState.textContent = "listening";
        startVisualizer();
    };

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        const lower = transcript.toLowerCase();
        let commandText = transcript;
        let hasWake = false;

        for (const wakeWord of wakeWords) {
            if (lower.includes(wakeWord)) {
                hasWake = true;
                commandText = transcript.replace(new RegExp(wakeWord, "gi"), "").trim();
                break;
            }
        }

        if (continuousMode && !hasWake) return;
        if (!commandText.trim()) return;

        const input = document.getElementById("text-input");
        if (input) input.value = commandText;
        if (window.handleSendCommand) window.handleSendCommand();
    };

    recognition.onerror = () => {
        document.getElementById("mic-button")?.classList.remove("listening");
        const voiceState = document.getElementById("runtime-voice");
        if (voiceState) voiceState.textContent = "ready";
        stopVisualizer();
    };

    recognition.onend = () => {
        const voiceState = document.getElementById("runtime-voice");
        if (voiceState) voiceState.textContent = continuousMode ? "listening" : "ready";

        if (continuousMode) {
            try {
                recognition.start();
            } catch {
                document.getElementById("mic-button")?.classList.remove("listening");
                stopVisualizer();
            }
            return;
        }

        document.getElementById("mic-button")?.classList.remove("listening");
        stopVisualizer();
    };
}

function startVisualizer() {
    const canvas = document.getElementById("audio-visualizer");
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    canvas.width = canvas.offsetWidth * 2;
    canvas.height = canvas.offsetHeight * 2;

    if (!audioContext) {
        try {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
            analyser = audioContext.createAnalyser();
            analyser.fftSize = 64;

            navigator.mediaDevices.getUserMedia({ audio: true })
                .then((stream) => {
                    const source = audioContext.createMediaStreamSource(stream);
                    source.connect(analyser);
                    drawVisualizer(ctx, canvas);
                })
                .catch(() => drawIdleVisualizer(ctx, canvas));
        } catch {
            drawIdleVisualizer(ctx, canvas);
        }
    } else {
        drawVisualizer(ctx, canvas);
    }
}

function drawVisualizer(ctx, canvas) {
    if (!analyser) return;

    const data = new Uint8Array(analyser.frequencyBinCount);

    function draw() {
        visualizerAnimId = requestAnimationFrame(draw);
        analyser.getByteFrequencyData(data);
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const bars = data.length;
        const barW = canvas.width / bars;
        const midY = canvas.height / 2;

        for (let i = 0; i < bars; i++) {
            const value = data[i] / 255;
            const height = value * midY * 0.92;
            const x = i * barW;
            const hue = i % 3 === 0 ? "143, 227, 193" : i % 3 === 1 ? "233, 184, 114" : "177, 145, 255";
            ctx.fillStyle = `rgba(${hue}, ${0.22 + value * 0.62})`;
            ctx.fillRect(x, midY - height, Math.max(2, barW - 2), height * 2);
        }
    }

    draw();
}

function drawIdleVisualizer(ctx, canvas) {
    function draw() {
        visualizerAnimId = requestAnimationFrame(draw);
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const bars = 24;
        const barW = canvas.width / bars;
        const midY = canvas.height / 2;
        const t = Date.now() / 760;

        for (let i = 0; i < bars; i++) {
            const value = 0.16 + Math.sin(t + i * 0.55) * 0.10 + Math.random() * 0.05;
            const height = value * midY;
            const hue = i % 2 === 0 ? "143, 227, 193" : "233, 184, 114";
            ctx.fillStyle = `rgba(${hue}, 0.34)`;
            ctx.fillRect(i * barW, midY - height, Math.max(2, barW - 3), height * 2);
        }
    }

    draw();
}

function stopVisualizer() {
    if (visualizerAnimId) {
        cancelAnimationFrame(visualizerAnimId);
        visualizerAnimId = null;
    }

    const canvas = document.getElementById("audio-visualizer");
    if (canvas) {
        const ctx = canvas.getContext("2d");
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
}

window.startVoiceRecognition = startVoiceRecognition;
window.toggleContinuousMode = toggleContinuousMode;
window.voiceSettings = voiceSettings;
window.loadVoices = loadVoices;
