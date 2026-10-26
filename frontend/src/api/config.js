// Ruta relativa: el propio servidor de Vite (ver vite.config.js) hace de proxy
// hacia el Gateway. Así funciona tanto en localhost como accediendo por una
// URL reenviada (Codespaces, devcontainer, etc.), sin problemas de CORS ni de host.
export const API_BASE_URL = "/api";
