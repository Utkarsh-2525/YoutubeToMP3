// React type declarations are not available in the current project setup.
// @ts-nocheck
import { FormEvent, useState } from "react";

type VideoInfo = {
  id: string;
  title: string;
  channel: string;
  thumbnail: string;
  duration: number;
};

const API = `${import.meta.env.VITE_API_URL}/api/youtube`;

function formatDuration(seconds: number) {
  if (!seconds) return "Unknown";

  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);

  return h
    ? `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
    : `${m}:${String(s).padStart(2, "0")}`;
}

function App() {
  const [url, setUrl] = useState("");
  const [info, setInfo] = useState<VideoInfo | null>(null);
  const [bitrate, setBitrate] = useState(320);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState("");

  async function fetchInfo(e?: FormEvent) {
    e?.preventDefault();

    if (!url.trim()) {
      setError("Paste a YouTube URL first.");
      return;
    }

    setLoading(true);
    setError("");
    setInfo(null);

    try {
      const response = await fetch(`${API}/info`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ url })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || "Could not fetch video information.");
      }

      setInfo(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed.");
    } finally {
      setLoading(false);
    }
  }

  async function downloadMp3() {
    if (!info) return;

    setDownloading(true);
    setError("");

    try {
      const response = await fetch(`${API}/download`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          url,
          bitrate
        })
      });

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.error || "Download failed.");
      }

      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = `${info.title.replace(/[\\/:*?"<>|]/g, "_")}.mp3`;
      document.body.appendChild(link);
      link.click();
      link.remove();

      URL.revokeObjectURL(objectUrl);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Download failed.");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <main className="page">
      <section className="app">
        <div className="brand">
          <div className="logo">♫</div>
          <div>
            <h1>YouTube → MP3</h1>
            <p>Convert permitted YouTube audio to MP3</p>
          </div>
        </div>

        <form className="url-form" onSubmit={fetchInfo}>
          <input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="Paste a YouTube URL..."
            type="url"
          />
          <button type="submit" disabled={loading}>
            {loading ? "Fetching..." : "Fetch Video"}
          </button>
        </form>

        {error && <div className="error">{error}</div>}

        {info && (
          <section className="video-card">
            <img
              src={info.thumbnail}
              alt={info.title}
              className="thumbnail"
            />

            <div className="video-details">
              <span className="label">VIDEO</span>
              <h2>{info.title}</h2>
              <p>{info.channel}</p>
              <span className="duration">
                {formatDuration(info.duration)}
              </span>
            </div>
          </section>
        )}

        {info && (
          <section className="controls">
            <label>
              Audio quality
              <select
                value={bitrate}
                onChange={(e) => setBitrate(Number(e.target.value))}
              >
                <option value={128}>128 kbps</option>
                <option value={192}>192 kbps</option>
                <option value={256}>256 kbps</option>
                <option value={320}>320 kbps</option>
              </select>
            </label>

            <button
              className="download"
              onClick={downloadMp3}
              disabled={downloading}
            >
              {downloading ? "Converting..." : "Download MP3"}
            </button>
          </section>
        )}

        <footer className="footer">
          <p>
            © {new Date().getFullYear()} Utkarsh Mishra. All rights reserved.
          </p>
          <p className="footer-note">
            Use only for content you own, have permission to download, or are
            otherwise legally allowed to use.
          </p>
        </footer>
      </section>
    </main>
  );
}

export default App;
