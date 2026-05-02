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
