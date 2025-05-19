function confirmRedirect(event) {
    event.preventDefault();
    if (confirm("To continue, you'll need to sign in via Google. Continue to login?")) {
        window.location.href = "/chat";
    }
}

function toggleTheme() {
    const body = document.body;
    const btn = document.getElementById('modeBtn');
    body.classList.toggle('dark');

    const isDark = body.classList.contains('dark');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
    btn.textContent = isDark ? 'Light Mode' : 'Dark Mode';
}

window.onload = () => {
    const savedTheme = localStorage.getItem('theme');
    const btn = document.getElementById('modeBtn');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark');
        btn.textContent = 'Light Mode';
    } else {
        btn.textContent = 'Dark Mode';
    }
}
