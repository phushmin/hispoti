// Answers where a song is from api.example.com, and why not when it cannot (feature "results").
var results = hispoti.features && hispoti.features.indexOf('results') >= 0;

hispoti.provide('audio-source', function (track) {
  hispoti.storage.set('lastTitle', track.title);
  var n = hispoti.storage.get('asked') || 0;
  hispoti.storage.set('asked', n + 1);
  var r = hispoti.http.get('https://api.example.com/find?title=' + encodeURIComponent(track.title), { timeout: 15000 });
  if (r.status === 429 && results) return { outcome: 'rate-limited', retryAfterMs: 1000 * Number(r.headers['retry-after'] || r.headers['Retry-After'] || 60) };
  if (r.status === 401 && results) return { outcome: 'sign-in', detail: 'Sign in again' };
  if (r.status >= 500 && results) return { outcome: 'server-down' };
  if (!r.ok) return null;
  return { url: r.json().url, mimeType: 'audio/flac' };
});
