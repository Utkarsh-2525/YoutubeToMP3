# YouTube → MP3 Converter

A local development project with:

- Spring Boot 3.4.4 / Java 21 backend
- React + TypeScript + Vite frontend
- yt-dlp for retrieving permitted YouTube audio
- FFmpeg for MP3 conversion
- 128/192/256/320 kbps selection
- Video metadata preview
- Local MP3 download

## 1. Install prerequisites

Install:

1. Java 21
2. Maven 3.9+
3. Node.js 20+
4. yt-dlp
5. FFmpeg

Place these two Windows executables here:

backend/tools/yt-dlp.exe
backend/tools/ffmpeg.exe

Or set YT_DLP_PATH and FFMPEG_PATH environment variables.

## 2. Run backend

Open a terminal:

cd backend
mvn spring-boot:run

Backend:
http://localhost:8080

## 3. Run frontend

Open another terminal:

cd frontend
npm install
npm run dev

Frontend:
http://localhost:5173

## 4. Test

Open:
http://localhost:5173

Paste a YouTube URL and click Fetch Video.

Then select bitrate and click Download MP3.

## API

POST /api/youtube/info

Body:
{"url":"https://www.youtube.com/watch?v=..."}

POST /api/youtube/download

Body:
{"url":"https://www.youtube.com/watch?v=...","bitrate":320}

The backend deliberately accepts only YouTube hostnames.

## Notes

- This is intended for local development.
- Do not add cookie/session bypasses or other mechanisms intended to circumvent access controls.
- Use only content you own, have permission to download, or are otherwise legally allowed to use.
- yt-dlp and FFmpeg must be kept up to date.
