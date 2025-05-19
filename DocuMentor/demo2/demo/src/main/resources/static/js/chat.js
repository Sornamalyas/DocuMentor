function showLoading() {
  document.getElementById('loading').style.display = 'block';
  document.querySelector('input[type="file"]').addEventListener('change', function () {
      const sendBtn = document.getElementById('sendBtn');
      if (this.files.length > 0) {
          sendBtn.disabled = true;
      }
  });
}
document.querySelector('input[type="file"]').addEventListener('change', function () {
    const file = this.files[0];
    const maxSize = 2 * 1024 * 1024; // 2MB

    if (file && file.size > maxSize) {
        alert("⚠️ File size exceeds 2 MB limit. Please upload a smaller file.");
        this.value = ''; // Clear the file input
        document.getElementById('sendBtn').disabled = true;
    } else if (file) {
        document.getElementById('sendBtn').disabled = false;
    }
});

function scrollToBottom() {
  const chatBox = document.getElementById("chat-box");
  chatBox.scrollTop = chatBox.scrollHeight;
}

async function ask() {
  const questionInput = document.getElementById("question");
  const chatBox = document.getElementById("chat-box");
  const typing = document.getElementById("typing");

  const question = questionInput.value.trim();
  if (!question) return;
  chatBox.innerHTML += `<div class="msg user">You: ${question}</div>`;
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
    chatBox.innerHTML += `<div class="msg bot">DocuMentor: ${answer}</div>`;
  } catch (err) {
    chatBox.innerHTML += `<div class="msg bot">Error: ${err.message}</div>`;
  }

  typing.style.display = "none";
  scrollToBottom();

}

async function reuseLast() {
  const chatBox = document.getElementById("chat-box");

  try {
    const res = await fetch("/useLast", { method: "POST" });
    const result = await res.text();

    if (result === "Unauthorized" || result === "No document present") {
      alert( result);
      return;
    }

    chatBox.innerHTML += `<div class="msg bot">Loaded file: ${result}</div>`;
    document.getElementById('sendBtn').disabled = false;
  } catch (err) {
    alert("Error: " + err.message);
  }

  scrollToBottom();
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
