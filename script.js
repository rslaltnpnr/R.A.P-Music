const audio = document.getElementById("audio");
const fileInput = document.getElementById("fileInput");
const playlistEl = document.getElementById("playlist");
const trackTitle = document.getElementById("trackTitle");
const trackMeta = document.getElementById("trackMeta");
const playBtn = document.getElementById("playBtn");
const prevBtn = document.getElementById("prevBtn");
const nextBtn = document.getElementById("nextBtn");
const shuffleBtn = document.getElementById("shuffleBtn");
const repeatBtn = document.getElementById("repeatBtn");
const progressBar = document.getElementById("progressBar");
const currentTimeEl = document.getElementById("currentTime");
const durationEl = document.getElementById("duration");
const volumeBar = document.getElementById("volumeBar");
const disc = document.getElementById("disc");

let playlist = [];
let currentIndex = -1;
let isShuffle = false;
let isRepeat = false;

function formatTime(seconds) {
  if (!isFinite(seconds)) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

function renderPlaylist() {
  playlistEl.innerHTML = "";
  playlist.forEach((track, index) => {
    const li = document.createElement("li");
    li.className = index === currentIndex ? "active" : "";
    li.innerHTML = `<span>${track.name}</span>`;
    const removeBtn = document.createElement("button");
    removeBtn.className = "remove-btn";
    removeBtn.textContent = "✕";
    removeBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      removeTrack(index);
    });
    li.appendChild(removeBtn);
    li.addEventListener("click", () => loadTrack(index, true));
    playlistEl.appendChild(li);
  });
}

function removeTrack(index) {
  const wasCurrent = index === currentIndex;
  URL.revokeObjectURL(playlist[index].url);
  playlist.splice(index, 1);

  if (playlist.length === 0) {
    currentIndex = -1;
    audio.pause();
    audio.removeAttribute("src");
    trackTitle.textContent = "Bir şarkı ekleyin";
    trackMeta.textContent = "—";
    disc.classList.remove("playing");
    playBtn.textContent = "▶";
  } else if (wasCurrent) {
    currentIndex = Math.min(index, playlist.length - 1);
    loadTrack(currentIndex, true);
  } else if (index < currentIndex) {
    currentIndex--;
  }
  renderPlaylist();
}

function loadTrack(index, autoplay) {
  if (index < 0 || index >= playlist.length) return;
  currentIndex = index;
  const track = playlist[currentIndex];
  audio.src = track.url;
  trackTitle.textContent = track.name;
  trackMeta.textContent = `Parça ${currentIndex + 1} / ${playlist.length}`;
  renderPlaylist();
  if (autoplay) {
    audio.play();
  }
}

function togglePlay() {
  if (playlist.length === 0) return;
  if (currentIndex === -1) {
    loadTrack(0, true);
    return;
  }
  if (audio.paused) {
    audio.play();
  } else {
    audio.pause();
  }
}

function playNext() {
  if (playlist.length === 0) return;
  let nextIndex;
  if (isShuffle) {
    nextIndex = Math.floor(Math.random() * playlist.length);
  } else {
    nextIndex = (currentIndex + 1) % playlist.length;
  }
  loadTrack(nextIndex, true);
}

function playPrev() {
  if (playlist.length === 0) return;
  const prevIndex = (currentIndex - 1 + playlist.length) % playlist.length;
  loadTrack(prevIndex, true);
}

fileInput.addEventListener("change", (e) => {
  const files = Array.from(e.target.files);
  files.forEach((file) => {
    playlist.push({ name: file.name.replace(/\.[^/.]+$/, ""), url: URL.createObjectURL(file) });
  });
  renderPlaylist();
  if (currentIndex === -1 && playlist.length > 0) {
    loadTrack(0, false);
  }
  fileInput.value = "";
});

playBtn.addEventListener("click", togglePlay);
nextBtn.addEventListener("click", playNext);
prevBtn.addEventListener("click", playPrev);

shuffleBtn.addEventListener("click", () => {
  isShuffle = !isShuffle;
  shuffleBtn.classList.toggle("active", isShuffle);
});

repeatBtn.addEventListener("click", () => {
  isRepeat = !isRepeat;
  repeatBtn.classList.toggle("active", isRepeat);
});

audio.addEventListener("play", () => {
  playBtn.textContent = "⏸";
  disc.classList.add("playing");
});

audio.addEventListener("pause", () => {
  playBtn.textContent = "▶";
  disc.classList.remove("playing");
});

audio.addEventListener("timeupdate", () => {
  if (audio.duration) {
    progressBar.value = (audio.currentTime / audio.duration) * 100;
    currentTimeEl.textContent = formatTime(audio.currentTime);
    durationEl.textContent = formatTime(audio.duration);
  }
});

audio.addEventListener("ended", () => {
  if (isRepeat) {
    audio.currentTime = 0;
    audio.play();
  } else {
    playNext();
  }
});

progressBar.addEventListener("input", () => {
  if (audio.duration) {
    audio.currentTime = (progressBar.value / 100) * audio.duration;
  }
});

volumeBar.addEventListener("input", () => {
  audio.volume = volumeBar.value / 100;
});

audio.volume = 1;
