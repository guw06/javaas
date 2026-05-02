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
    } catch (error) {
        hideTypingIndicator();
        addMessage('Произошла ошибка при обработке команды', false);
    } finally {
        // Включаем кнопку и инпут обратно
        sendButton.disabled = false;
        textInput.disabled = false;
        textInput.focus();
    }
}

sendButton.addEventListener('click', handleSendCommand);

textInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        handleSendCommand();
    }
});

// Приветственное сообщение
window.addEventListener('DOMContentLoaded', () => {
    addMessage('Здравствуйте! Я ваш голосовой ассистент. Чем могу помочь?', false);
});
