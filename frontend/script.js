const messagesContainer = document.getElementById('messages');
const textInput = document.getElementById('text-input');
const sendButton = document.getElementById('send-button');
const micButton = document.getElementById('mic-button');

function addMessage(text, isUser) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user-message' : 'assistant-message'}`;
    messageDiv.textContent = text;
    messagesContainer.appendChild(messageDiv);
    scrollToBottom();
}

function showTypingIndicator() {
    const typingDiv = document.createElement('div');
    typingDiv.className = 'typing-indicator';
    typingDiv.id = 'typing-indicator';
    typingDiv.innerHTML = `
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
    `;
    messagesContainer.appendChild(typingDiv);
    scrollToBottom();
}

function hideTypingIndicator() {
    const typingDiv = document.getElementById('typing-indicator');
    if (typingDiv) {
        typingDiv.remove();
    }
}

function scrollToBottom() {
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

async function handleSendCommand() {
    const text = textInput.value.trim();
    if (!text) return;

    // Отключаем кнопку и инпут
    sendButton.disabled = true;
    textInput.disabled = true;

    // Добавляем сообщение пользователя
    addMessage(text, true);
    textInput.value = '';

    // Показываем индикатор печати
    showTypingIndicator();

    try {
        // Отправляем команду на backend
        const response = await sendCommandToBackend(text);
        
        // Убираем индикатор печати
        hideTypingIndicator();
        
        // Добавляем ответ ассистента
        addMessage(response, false);
        
        // Озвучиваем ответ
        if (window.speak && voiceEnabled) {
            window.speak(response);
        }
    } catch (error) {
        hideTypingIndicator();
        console.error('Ошибка:', error);
        
        let errorMsg = 'Произошла ошибка при обработке команды';
        if (!navigator.onLine) {
            errorMsg = '❌ Нет подключения к интернету';
        } else if (!isOnline) {
            errorMsg = '❌ Сервер недоступен. Проверьте, запущен ли backend';
        }
        
        const errorDiv = document.createElement('div');
        errorDiv.className = 'message error-message';
        errorDiv.textContent = errorMsg;
        messagesContainer.appendChild(errorDiv);
        scrollToBottom();
    } finally {
        // Включаем кнопку и инпут обратно
        sendButton.disabled = false;
        textInput.disabled = false;
        textInput.focus();
    }
}

// Экспортируем функцию для использования в app.js
window.handleSendCommand = handleSendCommand;

sendButton.addEventListener('click', handleSendCommand);

textInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        handleSendCommand();
    }
});

// Приветственное сообщение
window.addEventListener('DOMContentLoaded', () => {
    addMessage('Здравствуйте! Я ваш голосовой ассистент. Чем могу помочь?', false);
    
    // Привязываем обработчик к кнопке микрофона
    micButton.addEventListener('click', () => {
        if (window.startVoiceRecognition) {
            window.startVoiceRecognition();
        }
    });
    
    // Двойной клик для включения continuous режима
    micButton.addEventListener('dblclick', () => {
        if (window.toggleContinuousMode) {
            const isEnabled = window.toggleContinuousMode();
            addMessage(
                isEnabled 
                    ? 'Режим постоянной прослушки включен. Используйте wake words: джарвис, алиса, ассистент' 
                    : 'Режим постоянной прослушки выключен',
                false
            );
        }
    });
});


// Кнопка для включения/выключения озвучивания
let voiceEnabled = true;

// Добавляем функции управления
window.toggleVoice = function() {
    voiceEnabled = !voiceEnabled;
    console.log('Озвучивание:', voiceEnabled ? 'включено' : 'выключено');
    return voiceEnabled;
};

window.getVoiceEnabled = function() {
    return voiceEnabled;
};


// Проверка подключения к серверу
let isOnline = true;
const connectionStatus = document.getElementById('connection-status');
const statusText = document.getElementById('status-text');
const modeIndicator = document.getElementById('mode-indicator');

async function checkConnection() {
    try {
        const response = await fetch('http://localhost:8080/ping', {
            method: 'GET',
            cache: 'no-cache'
        });
        
        if (response.ok) {
            if (!isOnline) {
                isOnline = true;
                updateConnectionStatus(true);
                addMessage('✅ Соединение восстановлено', false);
            }
        } else {
            throw new Error('Server error');
        }
    } catch (error) {
        if (isOnline) {
            isOnline = false;
            updateConnectionStatus(false);
            addMessage('❌ Потеряно соединение с сервером', false);
        }
    }
}

function updateConnectionStatus(online) {
    if (online) {
        connectionStatus.classList.remove('offline');
        connectionStatus.classList.add('online');
        statusText.textContent = 'Подключено';
    } else {
        connectionStatus.classList.remove('online');
        connectionStatus.classList.add('offline');
        statusText.textContent = 'Нет связи';
    }
}

// Проверяем соединение каждые 5 секунд
setInterval(checkConnection, 5000);

// Обновление индикатора режима
window.updateModeIndicator = function(continuousMode) {
    if (continuousMode) {
        modeIndicator.classList.add('active');
        micButton.classList.add('continuous-mode');
    } else {
        modeIndicator.classList.remove('active');
        micButton.classList.remove('continuous-mode');
    }
};

// Обработка ошибок сети
window.addEventListener('online', () => {
    updateConnectionStatus(true);
    addMessage('🌐 Интернет-соединение восстановлено', false);
});

window.addEventListener('offline', () => {
    updateConnectionStatus(false);
    addMessage('🌐 Нет подключения к интернету', false);
});

// Улучшенная обработка ошибок в handleSendCommand
const originalHandleSendCommand = window.handleSendCommand;
