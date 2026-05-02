// Инициализация Web Speech API
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
let continuousMode = false; // Режим постоянной прослушки
const wakeWords = ['джарвис', 'алиса', 'ассистент']; // Ключевые слова активации

if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.lang = 'ru-RU';
    recognition.continuous = false; // По умолчанию выключено
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
} else {
    console.error('Web Speech API не поддерживается в этом браузере');
}

async function sendCommandToBackend(text) {
    try {
        const response = await fetch('http://localhost:8080/api/command', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ text: text })
        });

        if (!response.ok) {
            throw new Error('Ошибка сети: ' + response.status);
        }

        const data = await response.json();
        return data.response;
    } catch (error) {
        console.error('Ошибка при отправке команды:', error);
        return 'Ошибка связи с сервером';
    }
}

// Функция синтеза речи
function speak(text) {
    if (!window.speechSynthesis) {
        console.error('Speech Synthesis API не поддерживается в этом браузере');
        return;
    }

    // Останавливаем предыдущую речь, если она есть
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ru-RU';
    utterance.rate = 1.0; // Скорость речи
    utterance.pitch = 1.0; // Высота тона
    utterance.volume = 1.0; // Громкость

    utterance.onstart = function() {
        console.log('Начало озвучивания');
    };

    utterance.onend = function() {
        console.log('Озвучивание завершено');
    };

    utterance.onerror = function(event) {
        console.error('Ошибка озвучивания:', event.error);
    };

    window.speechSynthesis.speak(utterance);
}

// Экспортируем функцию для использования в script.js
window.speak = speak;


// Функция для запуска распознавания речи
function startVoiceRecognition() {
    if (!recognition) {
        alert('Распознавание речи не поддерживается в вашем браузере');
        return;
    }

    try {
        recognition.start();
        console.log('Распознавание речи запущено');
    } catch (error) {
        console.error('Ошибка при запуске распознавания:', error);
    }
}

// Функция для переключения режима постоянной прослушки
function toggleContinuousMode() {
    if (!recognition) {
        alert('Распознавание речи не поддерживается в вашем браузере');
        return false;
    }

    continuousMode = !continuousMode;
    recognition.continuous = continuousMode;
    
    const micButton = document.getElementById('mic-button');
    
    if (continuousMode) {
        console.log('Режим постоянной прослушки ВКЛЮЧЕН');
        console.log('Используйте wake words:', wakeWords.join(', '));
        try {
            recognition.start();
            if (micButton) {
                micButton.classList.add('listening');
                micButton.classList.add('continuous-mode');
            }
            // Обновляем индикатор режима
            if (window.updateModeIndicator) {
                window.updateModeIndicator(true);
            }
        } catch (error) {
            console.error('Ошибка при запуске continuous режима:', error);
        }
    } else {
        console.log('Режим постоянной прослушки ВЫКЛЮЧЕН');
        try {
            recognition.stop();
            if (micButton) {
                micButton.classList.remove('listening');
                micButton.classList.remove('continuous-mode');
            }
            // Обновляем индикатор режима
            if (window.updateModeIndicator) {
                window.updateModeIndicator(false);
            }
        } catch (error) {
            console.error('Ошибка при остановке:', error);
        }
    }
    
    return continuousMode;
}

// Обработчики событий распознавания речи
if (recognition) {
    recognition.onstart = function() {
        console.log('Слушаю...');
        const micButton = document.getElementById('mic-button');
        if (micButton) {
            micButton.classList.add('listening');
        }
    };

    recognition.onresult = function(event) {
        const transcript = event.results[0][0].transcript;
        console.log('Распознано:', transcript);
        
        // Проверяем наличие wake word
        const lowerTranscript = transcript.toLowerCase();
        let commandText = transcript;
        let hasWakeWord = false;
        
        for (const wakeWord of wakeWords) {
            if (lowerTranscript.includes(wakeWord)) {
                hasWakeWord = true;
                // Вырезаем wake word из текста
                const regex = new RegExp(wakeWord, 'gi');
                commandText = transcript.replace(regex, '').trim();
                console.log('Wake word обнаружен:', wakeWord);
                console.log('Команда после обработки:', commandText);
                break;
            }
        }
        
        // В режиме постоянной прослушки обрабатываем только команды с wake word
        if (continuousMode && !hasWakeWord) {
            console.log('Wake word не обнаружен, игнорируем');
            return;
        }
        
        // Если команда пустая после удаления wake word, не отправляем
        if (!commandText || commandText.trim() === '') {
            console.log('Команда пустая после удаления wake word');
            return;
        }
        
        // Отправляем команду
        const textInput = document.getElementById('text-input');
        if (textInput) {
            textInput.value = commandText;
        }
        
        // Автоматически отправляем команду
        if (window.handleSendCommand) {
            window.handleSendCommand();
        }
    };

    recognition.onerror = function(event) {
        console.error('Ошибка распознавания:', event.error);
        const micButton = document.getElementById('mic-button');
        if (micButton) {
            micButton.classList.remove('listening');
        }
    };

    recognition.onend = function() {
        console.log('Распознавание завершено');
        const micButton = document.getElementById('mic-button');
        
        // В режиме постоянной прослушки автоматически перезапускаем
        if (continuousMode) {
            try {
                recognition.start();
                console.log('Перезапуск распознавания в continuous режиме');
            } catch (error) {
                console.error('Ошибка при перезапуске:', error);
                if (micButton) {
                    micButton.classList.remove('listening');
                }
            }
        } else {
            if (micButton) {
                micButton.classList.remove('listening');
            }
        }
    };
}


// Экспортируем функции для использования в script.js
window.startVoiceRecognition = startVoiceRecognition;
window.toggleContinuousMode = toggleContinuousMode;
