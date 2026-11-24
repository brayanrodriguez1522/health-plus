// HEALTH+ · Service worker mínimo (lo pide el celular para poder "instalar" la página como app).
// No guarda nada en caché: todo se pide siempre al servidor, así los datos están al día.
self.addEventListener('install', function () { self.skipWaiting(); });
self.addEventListener('activate', function (e) { e.waitUntil(self.clients.claim()); });
self.addEventListener('fetch', function () { /* sin caché: el navegador hace la petición normal */ });
