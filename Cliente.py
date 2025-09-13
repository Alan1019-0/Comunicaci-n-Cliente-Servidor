# Cliente.py
import socket
import threading
import json
import tkinter as tk
from tkinter import ttk, simpledialog, messagebox
import time

class Cliente:
    """
    Cliente gráfico con tkinter usando JSON como protocolo.
    - interfaz con Notebook: pestaña "General" + pestañas por privados
    - lista de usuarios en panel lateral
    - notificaciones: contador de mensajes privados no leídos y indicador "escribiendo"
    """

    def __init__(self, host="localhost", port=5000):
        self.host = host
        self.port = port
        self.sock = None
        self.usuario = None

        # GUI
        self.root = None
        self.notebook = None
        self.text_general = None
        self.entry = None
        self.users_listbox = None

        # mapas
        self.private_texts = {}      # usuario -> Text widget
        self.unread_counts = {}      # usuario -> int
        self.typing_label = None     # indicador "X está escribiendo..."
        self.last_typing_time = 0

        # hilo de escucha
        self.running = False

    # ----- conexión -----
    def conectar(self, nombre):
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.connect((self.host, self.port))
            self.usuario = nombre
            # enviar login
            self._send({"cmd": "login", "user": nombre})
            # iniciar hilo receptor
            self.running = True
            threading.Thread(target=self._receiving_loop, daemon=True).start()
            # crear GUI
            self._build_gui()
        except Exception as e:
            messagebox.showerror("Error conexión", f"No se pudo conectar: {e}")

    def cerrar(self):
        self.running = False
        try:
            if self.sock:
                self.sock.close()
        except:
            pass
        try:
            if self.root:
                self.root.destroy()
        except:
            pass

    # ----- red -----
    def _send(self, obj):
        try:
            if self.sock:
                self.sock.send(json.dumps(obj).encode('utf-8'))
        except Exception:
            pass

    def _receiving_loop(self):
        while self.running:
            try:
                raw = self.sock.recv(4096)
                if not raw:
                    break
                msg = json.loads(raw.decode('utf-8'))
                # procesar en hilo principal de tkinter
                if self.root:
                    self.root.after(0, self._handle_msg, msg)
                else:
                    self._handle_msg(msg)
            except Exception:
                break
        self.running = False
        # si se cae la conexión, avisar
        if self.root:
            try:
                self.root.after(0, lambda: messagebox.showwarning("Conexión", "Conexión con servidor perdida"))
            except:
                pass

    # ----- GUI -----
    def _build_gui(self):
        self.root = tk.Tk()
        self.root.title(f"Chat - {self.usuario}")
        self.root.geometry("820x520")

        # panel izquierdo: usuarios
        left = tk.Frame(self.root, width=180)
        left.pack(side="left", fill="y")
        tk.Label(left, text="Conectados").pack(pady=(6,0))
        self.users_listbox = tk.Listbox(left, width=20)
        self.users_listbox.pack(fill="y", expand=True, padx=6, pady=6)
        self.users_listbox.bind("<Double-Button-1>", self._open_private_tab)

        # indicador escribiendo
        self.typing_label = tk.Label(left, text="", fg="gray", font=("Arial", 9))
        self.typing_label.pack(pady=(0,6))

        # boton refrescar lista (opcional)
        tk.Button(left, text="Refrescar", command=lambda: self._send({"cmd": "users"})).pack(padx=6, pady=(0,6))

        # panel derecho: notebook
        right = tk.Frame(self.root)
        right.pack(side="right", fill="both", expand=True)

        self.notebook = ttk.Notebook(right)
        self.notebook.pack(fill="both", expand=True, padx=6, pady=6)
        # pestaña general
        frame_general = tk.Frame(self.notebook)
        self.text_general = tk.Text(frame_general, state="disabled", wrap="word")
        self.text_general.pack(fill="both", expand=True)
        self.notebook.add(frame_general, text="General")

        # abajo: entrada y botones
        bottom = tk.Frame(self.root)
        bottom.pack(side="bottom", fill="x", padx=6, pady=6)
        self.entry = tk.Entry(bottom)
        self.entry.pack(side="left", fill="x", expand=True, padx=(0,6))
        self.entry.bind("<Key>", self._on_typing)
        tk.Button(bottom, text="Enviar", command=self._on_send).pack(side="right")

        # cerrar correctamente
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)
        # iniciar loop
        self.root.mainloop()

    def _on_close(self):
        self._send({"cmd": "system_quit", "user": self.usuario})
        self.cerrar()

    # ----- interacciones -----
    def _on_send(self):
        texto = self.entry.get().strip()
        if not texto:
            return
        # detectar a qué pestaña se envía
        tab_text = self.notebook.tab(self.notebook.select(), "text")
        if tab_text == "General":
            self._send({"cmd": "broadcast", "msg": texto})
            # mostrar en local inmediatamente
            self._append_general(f"[{self.usuario}] {texto}")
        else:
            destino = tab_text
            self._send({"cmd": "message", "to": destino, "msg": texto})
            # mostrar en la pestaña privada (local)
            self._append_private(destino, f"[Tú] {texto}")
        self.entry.delete(0, "end")

    def _on_typing(self, event=None):
        now = time.time()
        # evitar enviar demasiados eventos
        if now - self.last_typing_time > 1.2:
            self._send({"cmd": "typing"})
            self.last_typing_time = now

    def _open_private_tab(self, event=None):
        sel = self.users_listbox.curselection()
        if not sel:
            return
        text = self.users_listbox.get(sel[0])
        # quitar contador si existe: "nombre (n)"
        if " (" in text:
            nombre = text.split(" (")[0]
        else:
            nombre = text
        self._ensure_private_tab(nombre)
        # marcar como leído
        self.unread_counts.pop(nombre, None)
        self._refresh_user_listbox()

    # ----- manejo mensajes entrantes -----
    def _handle_msg(self, msg):
        cmd = msg.get("cmd")
        if cmd == "broadcast":
            remitente = msg.get("from")
            texto = msg.get("msg")
            self._append_general(f"[{remitente}] {texto}")

        elif cmd == "private":
            remitente = msg.get("from")
            texto = msg.get("msg")
            # si pestaña abierta y visible, mostrar. Si no, incrementar contador.
            current_tab = self.notebook.tab(self.notebook.select(), "text")
            if remitente in self.private_texts and current_tab == remitente:
                self._append_private(remitente, f"[{remitente}] {texto}")
            else:
                # crear pestaña si no existe pero no seleccionarla
                self._ensure_private_tab(remitente)
                self.unread_counts[remitente] = self.unread_counts.get(remitente, 0) + 1
                self._refresh_user_listbox()

        elif cmd == "users":
            lista = msg.get("list", [])
            # actualizar listbox (sin incluirme)
            self._update_users(lista)

        elif cmd == "system":
            texto = msg.get("msg")
            self._append_general(f"* {texto}")

        elif cmd == "history":
            items = msg.get("items", [])
            # mostrar historial en general (al iniciar sesión)
            for it in items:
                ts = it.get('time', '')
                frm = it.get('from', '')
                m = it.get('msg', '')
                self._append_general(f"[{ts}] {frm}: {m}")

        elif cmd == "typing":
            user = msg.get("user")
            # mostrar temporalmente "user está escribiendo..."
            if user and user != self.usuario:
                self.typing_label.config(text=f"{user} está escribiendo...")
                # limpiar después de 2 segundos
                self.root.after(2000, lambda: self.typing_label.config(text=""))

        elif msg.get("status") == "ok":
            # mensajes de confirmación simple (opcional)
            pass
        elif msg.get("status") == "error":
            messagebox.showerror("Error", msg.get("msg", "Error"))

    # ----- utilidades GUI -----
    def _append_general(self, texto):
        self.text_general.config(state="normal")
        self.text_general.insert("end", texto + "\n")
        self.text_general.config(state="disabled")
        self.text_general.see("end")

    def _ensure_private_tab(self, nombre):
        if nombre in self.private_texts:
            # ya existe
            return
        frame = tk.Frame(self.notebook)
        ta = tk.Text(frame, state="disabled", wrap="word")
        ta.pack(fill="both", expand=True)
        self.notebook.add(frame, text=nombre)
        self.private_texts[nombre] = ta

    def _append_private(self, nombre, texto):
        self._ensure_private_tab(nombre)
        ta = self.private_texts[nombre]
        ta.config(state="normal")
        ta.insert("end", texto + "\n")
        ta.config(state="disabled")
        ta.see("end")

    def _update_users(self, lista):
        # no incluirme
        users = [u for u in lista if u != self.usuario]
        # sincronizar unread_counts keys (no borrar counters)
        for u in users:
            self.unread_counts.setdefault(u, 0)
        # remove those who left
        for u in list(self.unread_counts.keys()):
            if u not in users:
                self.unread_counts.pop(u)

        # actualizar visualmente
        self._refresh_user_listbox(users)

    def _refresh_user_listbox(self, forced_users=None):
        # forced_users permite pasar lista actual; si None, reconstrute from unread_counts keys
        if forced_users is None:
            users = list(self.unread_counts.keys())
        else:
            users = forced_users

        # orden alfabético
        users = sorted(users)
        self.users_listbox.delete(0, "end")
        for u in users:
            cnt = self.unread_counts.get(u, 0)
            label = f"{u}" + (f" ({cnt})" if cnt and cnt > 0 else "")
            self.users_listbox.insert("end", label)

    # ----- utilidad arranque rápido -----
    def start_gui_with_prompt(self):
        # pedir nombre con diálogo simple (sin consola)
        root = tk.Tk()
        root.withdraw()
        nombre = simpledialog.askstring("Usuario", "Ingresa tu nombre de usuario:", parent=root)
        root.destroy()
        if not nombre:
            return
        self.conectar(nombre)

# ----- arranque -----
if __name__ == "__main__":
    cliente = Cliente(host="localhost", port=5000)
    cliente.start_gui_with_prompt()
