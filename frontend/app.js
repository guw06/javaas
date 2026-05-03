// ===== Web Speech API — голос Джарвиса =====
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
let continuousMode = false;
const wakeWords = ['джарвис', 'алиса', 'ассистент'];

// Голосовые настройки
let voiceSettings = {
    voice: null,
    rate: 1.0,
    pitch: 1.0,
    volume: 1.0
};

// Audio контекст для визуализации
let audioContext = null;
let analyser = null;
let visualizerAnimId = null;

if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.lang = 'ru-RU';
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
} else {
    console.warn('Web Speech API не поддерживается');
}

// ===== Отправка команды на backend =====
async function sendCommandToBackend(text) {
    try {
        const response = await fetch('/api/command', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
        });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const data = await response.json();
        return data.response;
    } catch (error) {
        console.error('Ошибка:', error);
        return 'Ошибка связи с сервером';
    }
}

// ===== Синтез речи (TTS) =====
function speak(text) {
    if (!window.speechSynthesis) return;
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ru-RU';
    utterance.rate = voiceSettings.rate;
    utterance.pitch = voiceSettings.pitch;
    utterance.volume = voiceSettings.volume;
    if (voiceSettings.voice) utterance.voice = voiceSettings.voice;

    utterance.onstart = () => {
        document.getElementById('mic-button')?.classList.add('speaking');
    };
    utterance.onend = () => {
        document.getElementById('mic-button')?.classList.remove('speaking');
    };

    window.speechSynthesis.speak(utterance);
}
window.speak = speak;

// ===== Загрузка доступных голосов =====
function loadVoices() {
    const voices = window.speechSynthesis?.getVoices() || [];
    const select = document.getElementById('voice-select');
    if (!select) return;
    select.innerHTML = '';

    const ruVoices = voices.filter(v => v.lang.startsWith('ru'));
    const allVoices = ruVoices.length > 0 ? ruVoices : voices;

    allVoices.forEach((voice, i) => {
        const opt = document.createElement('option');
        opt.value = i;
        opt.textContent = `${voice.name} (${voice.lang})`;
        if (voice.default) opt.selected = true;
        select.appendChild(opt);
    });

    if (allVoices.length > 0 && !voiceSettings.voice) {
        voiceSettings.voice = allVoices[0];
    }

    select.addEventListener('change', () => {
        voiceSettings.voice = allVoices[parseInt(select.value)] || null;
    });
}
if (window.speechSynthesis) {
    speechSynthesis.onvoiceschanged = loadVoices;
    loadVoices();
}

// ===== Распознавание речи =====
function startVoiceRecognition() {
    if (!recognition) { alert('Распознавание речи не поддерживается'); return; }
    try { recognition.start(); } catch (e) { console.error(e); }
}

function toggleContinuousMode() {
    if (!recognition) return false;
    continuousMode = !continuousMode;
    recognition.continuous = continuousMode;
    const mic = document.getElementById('mic-button');
    const badge = document.getElementById('mode-badge');

    if (continuousMode) {
        try { recognition.start(); } catch (e) {}
        mic?.classList.add('continuous');
        badge?.classList.add('active');
    } else {
        try { recognition.stop(); } catch (e) {}
        mic?.classList.remove('continuous', 'listening');
        badge?.classList.remove('active');
    }
    return continuousMode;
}

if (recognition) {
    recognition.onstart = () => {
        document.getElementById('mic-button')?.classList.add('listening');
        startVisualizer();
    };

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        const lower = transcript.toLowerCase();
        let commandText = transcript;
        let hasWake = false;

        for (const ww of wakeWords) {
            if (lower.includes(ww)) {
                hasWake = true;
                commandText = transcript.replace(new RegExp(ww, 'gi'), '').trim();
                break;
            }
        }

        if (continuousMode && !hasWake) return;
        if (!commandText.trim()) return;

        const input = document.getElementById('text-input');
        if (input) input.value = commandText;
        if (window.handleSendCommand) window.handleSendCommand();
    };

    recognition.onerror = () => {
        document.getElementById('mic-button')?.classList.remove('listening');
        stopVisualizer();
    };

    recognition.onend = () => {
        if (continuousMode) {
            try { recognition.start(); } catch (e) {
                document.getElementById('mic-button')?.classList.remove('listening');
                stopVisualizer();
            }
        } else {
            document.getElementById('mic-button')?.classList.remove('listening');
            stopVisualizer();
        }
    };
}

// ===== Audio Visualizer =====
function startVisualizer() {
    const canvas = document.getElementById('audio-visualizer');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    canvas.width = canvas.offsetWidth * 2;
    canvas.height = canvas.offsetHeight * 2;

    if (!audioContext) {
        try {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
            analyser = audioContext.createAnalyser();
            analyser.fftSize = 64;
            navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
                const source = audioContext.createMediaStreamSource(stream);
                source.connect(analyser);
                drawVisualizer(ctx, canvas);
            }).catch(() => drawFakeVisualizer(ctx, canvas));
        } catch (e) { drawFakeVisualizer(ctx, canvas); }
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
            const val = data[i] / 255;
            const h = val * midY * 0.9;
            const x = i * barW;
            ctx.fillStyle = `rgba(0, 212, 255, ${0.3 + val * 0.7})`;
            ctx.fillRect(x, midY - h, barW - 1, h * 2);
        }
    }
    draw();
}

function drawFakeVisualizer(ctx, canvas) {
    function draw() {
        visualizerAnimId = requestAnimationFrame(draw);
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        const bars = 16;
        const barW = canvas.width / bars;
        const midY = canvas.height / 2;
        const t = Date.now() / 1000;

        for (let i = 0; i < bars; i++) {
            const val = 0.2 + Math.sin(t * 3 + i * 0.5) * 0.3 + Math.random() * 0.1;
            const h = val * midY * 0.8;
            ctx.fillStyle = `rgba(0, 212, 255, ${0.2 + val * 0.5})`;
            ctx.fillRect(i * barW, midY - h, barW - 2, h * 2);
        }
    }
    draw();
}

function stopVisualizer() {
    if (visualizerAnimId) {
        cancelAnimationFrame(visualizerAnimId);
        visualizerAnimId = null;
    }
    const canvas = document.getElementById('audio-visualizer');
    if (canvas) canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
}

// ===== Exports =====
window.startVoiceRecognition = startVoiceRecognition;
window.toggleContinuousMode = toggleContinuousMode;
window.voiceSettings = voiceSettings;
window.loadVoices = loadVoices;
