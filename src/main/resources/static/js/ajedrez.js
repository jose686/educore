/**
 * Minijuego interactivo de ajedrez (Reto Táctico) para el Aula Virtual.
 * Carga dinámicamente dependencias de Chess.js y Chessboard.js desde CDN.
 */
(function() {
    console.log("Cargando dependencias del minijuego de Ajedrez...");

    // Función auxiliar para inyectar hojas de estilo CSS
    function loadCSS(url) {
        if (!document.querySelector(`link[href="${url}"]`)) {
            const link = document.createElement("link");
            link.rel = "stylesheet";
            link.href = url;
            document.head.appendChild(link);
        }
    }

    // Función auxiliar para cargar scripts secuencialmente
    function loadScript(url, callback) {
        const script = document.createElement("script");
        script.type = "text/javascript";
        script.src = url;
        script.onload = callback;
        document.body.appendChild(script);
    }

    // 1. Cargar hoja de estilo del tablero
    loadCSS("https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/chessboard-1.0.0.min.css");

    // 2. Cargar jQuery (requerido por Chessboard.js)
    loadScript("https://code.jquery.com/jquery-3.7.1.min.js", function() {
        // 3. Cargar Chess.js para las reglas lógicas
        loadScript("https://cdnjs.cloudflare.com/ajax/libs/chess.js/0.10.3/chess.min.js", function() {
            // 4. Cargar Chessboard.js para renderizar la interfaz del tablero
            loadScript("https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/chessboard-1.0.0.min.js", function() {
                // Arrancar el minijuego una vez que todas las dependencias estén listas
                initializeChessGame();
            });
        });
    });

    /**
     * Inicialización del puzzle de Ajedrez interactivo
     */
    function initializeChessGame() {
        const container = document.getElementById("interactive-canvas");
        if (!container) return;

        // Limpiar el contenido de placeholder estático
        container.innerHTML = "";

        // Contenedor principal estilizado para el minijuego
        const wrapper = document.createElement("div");
        wrapper.className = "text-center p-3 w-100";
        wrapper.style.maxWidth = "420px";

        // Cabecera del reto
        const header = document.createElement("div");
        header.className = "mb-3";
        header.innerHTML = `
            <h5 class="fw-bold text-white mb-1"><i class="bi bi-trophy-fill text-warning me-1"></i> Reto: Mate en 1</h5>
            <p class="text-muted-custom fs-8 mb-0">Juegan las blancas. Encuentra el único movimiento ganador.</p>
        `;
        wrapper.appendChild(header);

        // Tablero de ajedrez
        const boardDiv = document.createElement("div");
        boardDiv.id = "chess-board";
        boardDiv.className = "mx-auto mb-3 shadow-lg rounded-3 border border-white border-opacity-10";
        boardDiv.style.width = "100%";
        wrapper.appendChild(boardDiv);

        // Alertas/Estados de la jugada
        const statusDiv = document.createElement("div");
        statusDiv.id = "chess-status";
        statusDiv.className = "alert alert-info border-0 bg-info bg-opacity-10 text-info py-2 px-3 fs-8 rounded-pill mt-2";
        statusDiv.innerText = "Tu turno: Mueve las blancas.";
        wrapper.appendChild(statusDiv);

        // Botón para resetear posición
        const resetBtn = document.createElement("button");
        resetBtn.className = "btn btn-outline-light border-white border-opacity-10 btn-sm rounded-pill mt-3 px-4 fs-8";
        resetBtn.innerHTML = "<i class='bi bi-arrow-counterclockwise me-1'></i> Reiniciar Reto";
        wrapper.appendChild(resetBtn);

        container.appendChild(wrapper);

        // FEN del Puzzle: Dama blanca en h6 y peón en f6 amenazando rey negro en h8.
        // El movimiento correcto es Qg7# (Mate).
        const puzzleFEN = "7k/8/5P1Q/5K2/8/8/8/8 w - - 0 1";
        const game = new Chess(puzzleFEN);

        function onDragStart(source, piece, position, orientation) {
            // Evitar mover piezas negras o si el puzzle ya terminó
            if (game.game_over() || piece.search(/^b/) !== -1) {
                return false;
            }
        }

        function onDrop(source, target) {
            // Intentar ejecutar el movimiento lógico
            const move = game.move({
                from: source,
                to: target,
                promotion: 'q'
            });

            // Si el movimiento no es legal, rebota
            if (move === null) return 'snapback';

            verifyOutcome();
        }

        function onSnapEnd() {
            board.position(game.fen());
        }

        function verifyOutcome() {
            if (game.in_checkmate()) {
                // El usuario acertó
                statusDiv.className = "alert alert-success border-0 bg-success bg-opacity-10 text-success py-2 px-3 fs-8 rounded-pill mt-2";
                statusDiv.innerHTML = "<strong>¡Excelente!</strong> ¡Jaque Mate! Reto completado con éxito. 🏆";
            } else {
                // Movimiento legal pero incorrecto (no da mate)
                statusDiv.className = "alert alert-danger border-0 bg-danger bg-opacity-10 text-danger py-2 px-3 fs-8 rounded-pill mt-2";
                statusDiv.innerText = "Jugada legal, pero no es mate en 1. Reintentando...";
                
                // Deshacer el intento fallido tras 1.5 segundos
                setTimeout(function() {
                    game.load(puzzleFEN);
                    board.position(puzzleFEN);
                    statusDiv.className = "alert alert-info border-0 bg-info bg-opacity-10 text-info py-2 px-3 fs-8 rounded-pill mt-2";
                    statusDiv.innerText = "Tu turno: Mueve las blancas.";
                }, 1500);
            }
        }

        // Instanciar Chessboard
        const board = Chessboard('chess-board', {
            draggable: true,
            position: puzzleFEN,
            onDragStart: onDragStart,
            onDrop: onDrop,
            onSnapEnd: onSnapEnd,
            pieceTheme: 'https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/img/chesspieces/wikipedia/{piece}.png'
        });

        // Evento de reinicio manual
        resetBtn.addEventListener("click", function() {
            game.load(puzzleFEN);
            board.position(puzzleFEN);
            statusDiv.className = "alert alert-info border-0 bg-info bg-opacity-10 text-info py-2 px-3 fs-8 rounded-pill mt-2";
            statusDiv.innerText = "Tu turno: Mueve las blancas.";
        });

        // Mantener tablero responsivo
        window.addEventListener('resize', board.resize);
    }
})();
