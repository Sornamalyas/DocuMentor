function updateFileName() {
    const fileInput = document.getElementById("file-upload");
    const fileName = document.getElementById("file-name");
    const sendBtn = document.getElementById("send-btn");

    if (fileInput.files.length > 0) {
        fileName.textContent = fileInput.files[0].name;
        sendBtn.disabled = false;
    } else {
        fileName.textContent = "";
        sendBtn.disabled = true;
    }
}

function showLoading() {
    document.getElementById('loading').style.display = 'block';
}

async function ask() {
    const questionInput = document.getElementById("question");
    const chatBox = document.getElementById("chat-box");
    const typing = document.getElementById("typing");

    const question = questionInput.value.trim();
    if (!question) return;

    chatBox.innerHTML += `<div class="msg user"><div class="bubble">You: ${question}</div></div>`;
    questionInput.value = "";
    typing.style.display = "block";

    try {
        const res = await fetch("/ask", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: `question=${encodeURIComponent(question)}`
        });

        const answer = await res.text();
        chatBox.innerHTML += `<div class="msg bot"><div class="bubble">DocuMentor: ${answer}</div></div>`;
    } catch (err) {
        chatBox.innerHTML += `<div class="msg bot">Error: ${err.message}</div>`;
    }

    typing.style.display = "none";
    chatBox.scrollTop = chatBox.scrollHeight;
}
async function reuseLast() {
  const chatBox = document.getElementById("chat-box");

  try {
    const res = await fetch("/useLast", {
      method: "POST"
    });

    const result = await res.text();

    if (result === "Unauthorized" || result === "No document present") {
      alert("⚠️ " + result);
      return;
    }

function logout() {
    window.location.href = "/logout";
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
    }
};
